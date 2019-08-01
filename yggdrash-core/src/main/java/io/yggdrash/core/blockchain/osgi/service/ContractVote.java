package io.yggdrash.core.blockchain.osgi.service;

import java.io.Serializable;

public class ContractVote implements Serializable {
    private String txId;
    private boolean isAgree;

    public ContractVote() {

    }

    public ContractVote(String txId, boolean isAgree) {
        this.txId = txId;
        this.isAgree = isAgree;
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
}
