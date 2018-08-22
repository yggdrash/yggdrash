package io.yggdrash.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContractParam {
    private String operator;
    private String chainName;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    @JsonIgnore
    public boolean isGenesisOp() {
        return operator != null && "GENESIS".equals(operator);
    }
}
