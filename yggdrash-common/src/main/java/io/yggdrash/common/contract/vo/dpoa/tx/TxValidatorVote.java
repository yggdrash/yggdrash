package io.yggdrash.common.contract.vo.dpoa.tx;

import io.yggdrash.common.contract.SerialEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TxValidatorVote implements Serializable, TxPayload {
    private static final long serialVersionUID = SerialEnum.TX_VALIDATOR_VOTE.toValue();

    private String validatorAddr;
    private boolean isAgree;

    public TxValidatorVote() {

    }

    public TxValidatorVote(String validatorAddr, boolean isAgree) {
        this.validatorAddr = validatorAddr;
        this.isAgree = isAgree;
    }

    public String getValidatorAddr() {
        return validatorAddr;
    }

    public void setValidatorAddr(String validatorAddr) {
        this.validatorAddr = validatorAddr;
    }

    public boolean isAgree() {
        return isAgree;
    }

    public void setAgree(boolean agree) {
        isAgree = agree;
    }

    @Override
    public boolean validate() {
        if (StringUtils.isEmpty(validatorAddr)) {
            return false;
        }
        return true;
    }
}
