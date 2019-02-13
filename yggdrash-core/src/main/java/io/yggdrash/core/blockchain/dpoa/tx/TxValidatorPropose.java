package io.yggdrash.core.blockchain.dpoa.tx;

import io.yggdrash.core.blockchain.SerialEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TxValidatorPropose implements Serializable, TxPayload {
    private static final long serialVersionUID = SerialEnum.TX_VALIDATOR_PROPOSE.toValue();

    private String validatorAddr;

    public TxValidatorPropose() {

    }

    public TxValidatorPropose(String validatorAddr) {
        this.validatorAddr = validatorAddr;
    }

    public String getValidatorAddr() {
        return validatorAddr;
    }

    public void setValidatorAddr(String validatorAddr) {
        this.validatorAddr = validatorAddr;
    }

    @Override
    public boolean validate() {
        if (StringUtils.isEmpty(validatorAddr)) {
            return false;
        }
        return true;
    }
}
