package io.yggdrash.common.contract;

import io.yggdrash.common.contract.vo.dpoa.tx.TxPayload;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TxContractUpdatePropose implements Serializable, TxPayload {
    private String contract;

    public TxContractUpdatePropose() {

    }

    public TxContractUpdatePropose(String contract) {
        this.contract = contract;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    @Override
    public boolean validate() {
        return !StringUtils.isEmpty(contract);
    }
}
