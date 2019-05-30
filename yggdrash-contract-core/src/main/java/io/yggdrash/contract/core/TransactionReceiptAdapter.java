package io.yggdrash.contract.core;

import java.util.List;

public class TransactionReceiptAdapter implements TransactionReceipt {

    TransactionReceipt tr;

    public void setTransactionReceipt(TransactionReceipt tr) {
        this.tr = tr;
    }

    @Override
    public void addLog(String log) {
        this.tr.addLog(log);
    }

    @Override
    public ExecuteStatus getStatus() {
        return tr.getStatus();
    }

    @Override
    public void setStatus(ExecuteStatus status) {
        this.tr.setStatus(status);
    }

    @Override
    public String getTxId() {
        return this.tr.getTxId();
    }

    @Override
    public Long getTxSize() {
        return this.tr.getTxSize();
    }

    @Override
    public void setTxId(String txId) {
        // adapter can not set TxId
    }

    @Override
    public String getBlockId() {
        return this.tr.getBlockId();
    }

    @Override
    public void setBlockId(String blockId) {
        // adapter can not set blockId
    }

    @Override
    public Long getBlockHeight() {
        return this.tr.getBlockHeight();
    }

    @Override
    public void setBlockHeight(Long blockHeight) {
        // adapter can not set TxId
    }

    @Override
    public String getBranchId() {
        return this.tr.getBranchId();
    }

    @Override
    public void setBranchId(String branchId) {
        // adapter can not set branchId
    }

    @Override
    public String getContractVersion() {
        return this.tr.getContractVersion();
    }

    @Override
    public void setContractVersion(String contractId) {
        // adapter can not set contractVersion
    }

    @Override
    public List<String> getTxLog() {
        return this.tr.getTxLog();
    }

    @Override
    public boolean isSuccess() {
        return this.tr.isSuccess();
    }

    @Override
    public String getIssuer() {
        return this.tr.getIssuer();
    }

    @Override
    public void setIssuer(String issuer) {
        // adapter can not set issuer
    }
}
