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
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InjectEvent;
import io.yggdrash.contract.core.annotation.InjectOutputStore;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.annotation.ParamValidation;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.apache.commons.collections4.MapUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        private final String validatorSchemeName = "validator";
        private final String proposedValidatorSchemeName = "proposedValidator";
        @InjectOutputStore
        Map<OutputType, OutputStore> outputStore;
        @InjectEvent
        Set<String> eventStore;


        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @ContractBranchStateStore
        BranchStateStore branchStateStore;

        @Genesis
        @ParamValidation
        @InvokeTransaction
        public TransactionReceipt init(JsonObject params) {
            log.info("Initialize DPoA");
            boolean isSuccess = saveInitValidator(params.getAsJsonArray("validators"));
            txReceipt.setStatus(isSuccess ? ExecuteStatus.SUCCESS : ExecuteStatus.FALSE);
            return txReceipt;
        }

        public boolean saveInitValidator(JsonArray validators) {
            ValidatorSet validatorSet = getValidatorSet();
            if (validatorSet != null) {
                return true;
            }

            validatorSet = new ValidatorSet();
            for (int i = 0; i < validators.size(); i++) {
                validatorSet.getValidatorMap().put(validators.get(i).getAsString(),
                        new Validator(validators.get(i).getAsString()));
            }
            JsonObject jsonObject = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
            state.put(PrefixKeyEnum.VALIDATORS.toValue(), jsonObject);
            return true;
        }

        private void sendOutputStore(String schemeName, String id, JsonObject jsonObject) {
            if (outputStore == null || eventStore == null || !eventStore.contains(validatorSchemeName)) {
                return;
            }
            outputStore.forEach((outputType, store) -> store.put(schemeName, id, jsonObject));
        }

        private boolean validateTx(TxPayload txPayload) {
            if (txPayload == null || !txPayload.validate()) {
                txReceipt.setStatus(ExecuteStatus.ERROR);
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
            ValidatorSet validatorSet = null;
            JsonObject jsonValidatorSet = state.get(PrefixKeyEnum.VALIDATORS.toValue());
            if (jsonValidatorSet != null) {
                validatorSet = JsonUtil.generateJsonToClass(jsonValidatorSet.toString(), ValidatorSet.class);
            }

            return validatorSet;
        }

        @InvokeTransaction
        public TransactionReceipt proposeValidator(JsonObject params) {
            txReceipt.setStatus(ExecuteStatus.FALSE);

            //Check validation
            TxValidatorPropose txValidatorPropose = JsonUtil.generateJsonToClass(params.toString(),
                    TxValidatorPropose.class);
            if (!validateTx(txValidatorPropose)) {
                return txReceipt;
            }

            //Is exists proposer
            ValidatorSet validatorSet = getValidatorSet();
            if (validatorSet == null || validatorSet.getValidatorMap() == null
                    || validatorSet.getValidatorMap().get(txReceipt.getIssuer()) == null) {
                return txReceipt;
            }

            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
                //Is exists validator in propose set
                if (proposeValidatorSet.getValidatorMap().get(txValidatorPropose.getValidatorAddr()) != null) {
                    return txReceipt;
                }

                //Is the proposed Validator voting complete
                for (String s : proposeValidatorSet.getValidatorMap().keySet()) {
                    if (txReceipt.getIssuer().equals(
                            proposeValidatorSet.getValidatorMap().get(s).getProposalValidatorAddr())) {
                        return txReceipt;
                    }
                }
            }

            //Add propose validator
            if (proposeValidatorSet == null) {
                proposeValidatorSet = new ProposeValidatorSet();
            }
            ProposeValidatorSet.Votable votable = new ProposeValidatorSet.Votable(txReceipt.getIssuer(), validatorSet);
            proposeValidatorSet.getValidatorMap().put(txValidatorPropose.getValidatorAddr(), votable);

            //Save
            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            return txReceipt;
        }

        @InvokeTransaction
        public TransactionReceipt voteValidator(JsonObject params) {
            txReceipt.setStatus(ExecuteStatus.FALSE);

            //Check validation
            TxValidatorVote txValidatorVote = JsonUtil.generateJsonToClass(params.toString(), TxValidatorVote.class);
            if (!validateTx(txValidatorVote)) {
                return txReceipt;
            }

            //Is exists proposed validator
            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet == null || MapUtils.isEmpty(proposeValidatorSet.getValidatorMap())
                    || proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr()) == null) {
                return txReceipt;
            }

            //Check available vote
            ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap()
                    .get(txValidatorVote.getValidatorAddr());
            if (votable.getVotedMap().get(txReceipt.getIssuer()) == null
                    || votable.getVotedMap().get(txReceipt.getIssuer()).isVoted()) {
                return txReceipt;
            }

            //Vote
            if (txValidatorVote.isAgree()) {
                votable.setAgreeCnt(votable.getAgreeCnt() + 1);
            } else {
                votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
            }
            votable.getVotedMap().get(txReceipt.getIssuer()).setAgree(txValidatorVote.isAgree());
            votable.getVotedMap().get(txReceipt.getIssuer()).setVoted(true);

            //Save
            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            return txReceipt;
        }

        // TODO should receive a set of byzantine and a set of validator that participated in the
        // previous block consensus.
        @ContractEndBlock
        public List<Validator> commit(JsonObject params) {
            boolean isUpdateValidator = false;
            boolean isUpdateProposedValidator = false;
            ValidatorSet validatorSet = getValidatorSet();

            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
            if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
                for (Iterator<Map.Entry<String, ProposeValidatorSet.Votable>> it =
                        proposeValidatorSet.getValidatorMap().entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ProposeValidatorSet.Votable> entry = it.next();
                    switch (entry.getValue().status()) {
                        case AGREE:
                            if (validatorSet.getValidatorMap().get(entry.getKey()) == null) {
                                isUpdateValidator = true;
                                Validator validator = new Validator(entry.getKey(), entry.getValue());
                                validatorSet.getValidatorMap().put(entry.getKey(), validator);
                            }
                        case DISAGREE:
                            it.remove();
                            isUpdateProposedValidator = true;
                            break;
                        default:
                            break;
                    }
                }
            }

            JsonObject jsonValidator = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
            if (isUpdateValidator) {
                state.put(PrefixKeyEnum.VALIDATORS.toValue(), jsonValidator);
            }
            JsonObject proposedValidator = null;
            if (proposeValidatorSet != null) {
                proposedValidator = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet));
            }
            if (isUpdateProposedValidator) {
                state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), proposedValidator);
            }

            List<Validator> validators = validatorSet.order(null);
            if (params != null) {
                if (jsonValidator != null) {
                    sendOutputStore(validatorSchemeName, params.get("blockNo").getAsString(), jsonValidator);
                }
                if (proposedValidator != null) {
                    sendOutputStore(proposedValidatorSchemeName, params.get("blockNo").getAsString(),
                            proposedValidator);
                }
            }
            return validators;
        }

        //todo need to set governance
        @InvokeTransaction
        public TransactionReceipt recoverValidator(String recoverValidator) {
            return null;
        }
    }
}
