/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract.dpoa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.contract.vo.dpoa.tx.TxPayload;
import io.yggdrash.common.contract.vo.dpoa.tx.TxValidatorPropose;
import io.yggdrash.common.contract.vo.dpoa.tx.TxValidatorVote;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InjectEvent;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.annotation.ParamValidation;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.apache.commons.collections4.MapUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet.Votable.VoteStatus.AGREE;
import static io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet.Votable.VoteStatus.NOT_YET;

public class DPoAContract implements BundleActivator {
    private static final Logger log = LoggerFactory.getLogger(DPoAContract.class);

    @Override
    public void start(BundleContext context) {
        log.info("Start dpoa contract");

        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "DPoA");
        context.registerService(DPoAService.class.getName(), new DPoAService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop dpoa contract");
    }

    public static class DPoAService {
        private static final String validatorSchemeName = "validator";
        private static final String proposedValidatorSchemeName = "proposedValidator";

        @InjectEvent
        Set<String> eventStore;

        @ContractChannelField
        public ContractChannel channel;

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractReceipt
        Receipt receipt;

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        @Genesis
        @ParamValidation
        @InvokeTransaction
        public Receipt init(JsonObject params) {
            log.info("Initialize DPoA");
            boolean isSuccess = saveInitValidator(params.getAsJsonArray("validators"));
            receipt.setStatus(isSuccess ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);
            return receipt;
        }

        public boolean saveInitValidator(JsonArray validators) {
            log.debug("saveInitValidator {}", validators);
            ValidatorSet validatorSet = getValidatorSet();
            if (validatorSet != null) {
                log.error("initial validator is not null");
                return false;
            }

            validatorSet = new ValidatorSet();
            Map<String, Validator> validatorMap = new HashMap<>();
            for (int i = 0; i < validators.size(); i++) {
                validatorMap.put(validators.get(i).getAsString(),
                        new Validator(validators.get(i).getAsString()));
            }
            validatorSet.setValidatorMap(validatorMap);

            branchStateStore.setValidators(validatorSet);

            return true;
        }

        private boolean validateTx(TxPayload txPayload) {
            if (txPayload == null || !txPayload.validate()) {
                receipt.setStatus(ExecuteStatus.ERROR);
                return false;
            }
            return true;
        }

        public ProposeValidatorSet getProposeValidatorSet() {
            ProposeValidatorSet proposeValidatorSet = null;
            JsonObject jsonProposeValidatorSet = state.get(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue());
            if (jsonProposeValidatorSet != null) {
                proposeValidatorSet = JsonUtil.generateJsonToClass(jsonProposeValidatorSet.toString(),
                        ProposeValidatorSet.class);
            }

            return proposeValidatorSet;
        }

        @ContractQuery
        public ValidatorSet getValidatorSet() {
            return branchStateStore.getValidators();
        }

        @ContractQuery
        public boolean isValidator(JsonObject params) {
            String address = params.get("address").getAsString();
            return this.branchStateStore.isValidator(address);
        }

        @InvokeTransaction
        public Receipt proposeValidator(JsonObject params) {
            receipt.setStatus(ExecuteStatus.FALSE);
            log.debug("proposeValidator {}", params);
            //Check validation
            TxValidatorPropose txValidatorPropose = JsonUtil.generateJsonToClass(params.toString(),
                    TxValidatorPropose.class);
            if (!validateTx(txValidatorPropose)) {
                return receipt;
            }

            //Is exists proposer
            ValidatorSet validatorSet = getValidatorSet();
            if (validatorSet == null || validatorSet.getValidatorMap() == null
                    || validatorSet.getValidatorMap().get(receipt.getIssuer()) == null) {
                log.error("ISSUER IS NOT validator {}", receipt.getIssuer());
                return receipt;
            }

            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
                //Is exists validator in propose set
                if (proposeValidatorSet.getValidatorMap().get(txValidatorPropose.getValidatorAddr()) != null) {
                    return receipt;
                }

                //Is the proposed Validator voting complete
                for (String s : proposeValidatorSet.getValidatorMap().keySet()) {
                    if (receipt.getIssuer().equals(
                            proposeValidatorSet.getValidatorMap().get(s).getProposalValidatorAddr())) {
                        return receipt;
                    }
                }
            }

            //Add propose validator
            if (proposeValidatorSet == null) {
                proposeValidatorSet = new ProposeValidatorSet();
            }
            ProposeValidatorSet.Votable votable = new ProposeValidatorSet.Votable(receipt.getIssuer(), validatorSet);
            proposeValidatorSet.getValidatorMap().put(txValidatorPropose.getValidatorAddr(), votable);

            //Save
            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
            receipt.setStatus(ExecuteStatus.SUCCESS);
            return receipt;
        }

        @InvokeTransaction
        public Receipt voteValidator(JsonObject params) {
            receipt.setStatus(ExecuteStatus.FALSE);

            //Check validation
            TxValidatorVote txValidatorVote = JsonUtil.generateJsonToClass(params.toString(), TxValidatorVote.class);
            if (!validateTx(txValidatorVote)) {
                return receipt;
            }

            //Is exists proposed validator
            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet == null || MapUtils.isEmpty(proposeValidatorSet.getValidatorMap())
                    || proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr()) == null) {
                return receipt;
            }

            //Check available vote
            ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap()
                    .get(txValidatorVote.getValidatorAddr());
            if (votable.getVotedMap().get(receipt.getIssuer()) == null
                    || votable.getVotedMap().get(receipt.getIssuer()).isVoted()) {
                return receipt;
            }

            //Vote
            if (txValidatorVote.isAgree()) {
                votable.setAgreeCnt(votable.getAgreeCnt() + 1);
            } else {
                votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
            }
            votable.getVotedMap().get(receipt.getIssuer()).setAgree(txValidatorVote.isAgree());
            votable.getVotedMap().get(receipt.getIssuer()).setVoted(true);

            //Save
            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
            receipt.setStatus(ExecuteStatus.SUCCESS);
            return receipt;
        }

        // TODO should receive a set of byzantine and a set of validator that participated in the
        // previous block consensus.
        @ContractEndBlock
        public List<Validator> commit() {
            boolean isUpdateValidator = false;
            boolean isUpdateProposedValidator = false;
            ValidatorSet validatorSet = getValidatorSet();

            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
                for (Iterator<Map.Entry<String, ProposeValidatorSet.Votable>> it = proposeValidatorSet
                        .getValidatorMap().entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ProposeValidatorSet.Votable> entry = it.next();
                    if (AGREE == entry.getValue().status()
                            && validatorSet.getValidatorMap().get(entry.getKey()) == null) {
                        isUpdateValidator = true;
                        Validator validator = new Validator(entry.getKey(), entry.getValue());
                        validatorSet.getValidatorMap().put(entry.getKey(), validator);
                    }
                    if (entry.getValue().status() != NOT_YET) {
                        it.remove();
                        isUpdateProposedValidator = true;
                    }
                }
            }

            if (isUpdateValidator) {
                // Save
                branchStateStore.setValidators(validatorSet);
            }
            JsonObject proposedValidator = null;
            if (proposeValidatorSet != null) {
                proposedValidator = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet));
            }
            if (isUpdateProposedValidator) {
                state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), proposedValidator);
            }

            return getValidatorSet().order(null);
        }

        //todo need to set governance
        @InvokeTransaction
        public Receipt recoverValidator(String recoverValidator) {
            return null;
        }

        @ContractEndBlock
        public Receipt endBlock() {
            return receipt;
        }
    }
}