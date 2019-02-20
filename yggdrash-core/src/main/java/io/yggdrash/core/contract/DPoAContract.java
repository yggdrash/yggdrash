package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.PrefixKeyEnum;
import io.yggdrash.core.blockchain.dpoa.ProposeValidatorSet;
import io.yggdrash.core.blockchain.dpoa.Validator;
import io.yggdrash.core.blockchain.dpoa.ValidatorSet;
import io.yggdrash.core.blockchain.dpoa.tx.TxPayload;
import io.yggdrash.core.blockchain.dpoa.tx.TxValidatorPropose;
import io.yggdrash.core.blockchain.dpoa.tx.TxValidatorVote;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;
import io.yggdrash.core.runtime.annotation.ParamValidation;
import io.yggdrash.core.store.Store;
import org.apache.commons.collections4.MapUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DPoAContract implements Contract<JsonObject> {
    @ContractStateStore
    Store<String, JsonObject> state;

    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @Genesis
    @ParamValidation
    @InvokeTransaction
    public TransactionReceipt init(JsonObject params) {
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
        for (JsonElement validator : validators) {
            validatorSet.getValidatorMap().put(validator.getAsString(),
                    new Validator(validator.getAsString()));
        }
        JsonObject jsonObject = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
        state.put(PrefixKeyEnum.VALIDATORS.toValue(), jsonObject);
        return true;
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
            proposeValidatorSet = JsonUtil.generateJsonToClass(
                    jsonProposeValidatorSet.toString(), ProposeValidatorSet.class);
        }

        return proposeValidatorSet;
    }

    public ValidatorSet getValidatorSet() {
        ValidatorSet validatorSet = null;
        JsonObject jsonValidatorSet = state.get(PrefixKeyEnum.VALIDATORS.toValue());
        if (jsonValidatorSet != null) {
            validatorSet = JsonUtil.generateJsonToClass(
                    jsonValidatorSet.toString(), ValidatorSet.class);
        }

        return validatorSet;
    }

    @InvokeTransaction
    public TransactionReceipt proposeValidator(JsonObject params) {
        txReceipt.setStatus(ExecuteStatus.FALSE);

        //Check validation
        TxValidatorPropose txValidatorPropose = JsonUtil.generateJsonToClass(
                params.toString(), TxValidatorPropose.class);
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
                if (txReceipt.getIssuer().equals(proposeValidatorSet.getValidatorMap()
                        .get(s).getProposalValidatorAddr())) {
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
        state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), JsonUtil.parseJsonObject(
                JsonUtil.convertObjToString(proposeValidatorSet)));
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
                || proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr())
                == null) {
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
        state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), JsonUtil.parseJsonObject(
                JsonUtil.convertObjToString(proposeValidatorSet)));
        txReceipt.setStatus(ExecuteStatus.SUCCESS);
        return txReceipt;
    }

    //todo should receive a set of byzantine and a set of validator that participated in the previous block consensus.
    public List<Validator> commit() {
        boolean isUpdateValidator = false;
        boolean isUpdateProposedValidator = false;
        ValidatorSet validatorSet = getValidatorSet();

        ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
        if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
            for (Iterator<Map.Entry<String, ProposeValidatorSet.Votable>> it = proposeValidatorSet
                    .getValidatorMap().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ProposeValidatorSet.Votable> entry = it.next();
                switch (entry.getValue().status()) {
                    case AGREE:
                        if (validatorSet.getValidatorMap().get(entry.getKey()) == null) {
                            isUpdateValidator = true;
                            Validator validator = new Validator(entry.getKey(), entry.getValue());
                            validatorSet.getValidatorMap().put(entry.getKey(), validator);
                        }
                    case DISAGREE:
                        // both AGREE and DISAGREE remove and update validator
                        it.remove();
                        isUpdateProposedValidator = true;
                        break;
                    default:
                        break;
                }
            }
        }

        if (isUpdateValidator) {
            state.put(PrefixKeyEnum.VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet)));
        }
        if (isUpdateProposedValidator) {
            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(),
                    JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
        }

        return validatorSet.order(null);
    }

    //todo need to set governance
    @InvokeTransaction
    public TransactionReceipt recoverValidator(String recoverValidator) {
        return null;
    }
}
