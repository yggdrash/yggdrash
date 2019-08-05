package io.yggdrash.core.blockchain.osgi.service;

import java.io.Serializable;

public class ContractVote implements Serializable {
    private String txId;
    private boolean isAgree;
    private String method;
    private String contractVersion;

    public ContractVote() {

    }

    public ContractVote(String txId, boolean isAgree, String method, String contractVersion) {
        this.txId = txId;
        this.isAgree = isAgree;
        this.method = method;
        this.contractVersion = contractVersion;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public boolean isAgree() {
        return isAgree;
    }

    public void setAgree(boolean agree) {
        isAgree = agree;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public void setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
    }
}
