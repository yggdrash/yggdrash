package io.yggdrash.common.contract;

import io.yggdrash.common.contract.vo.dpoa.tx.TxPayload;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TxContractUpdatePropose implements Serializable, TxPayload {
    private String contractVersion;
    private String contract;

    public TxContractUpdatePropose() {

    }

    public TxContractUpdatePropose(String contractVersion, String contract) {
        this.contract = contract;
        this.contractVersion = contractVersion;

    }

    public String getContractVersion() {
        return contractVersion;
    }

    public String getContract() {
        return contract;
    }

    public void setContractVersion(String contractVersion) {
        this.contract = contractVersion;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    @Override
    public boolean validate() {
        return !StringUtils.isEmpty(contract);
    }
}
