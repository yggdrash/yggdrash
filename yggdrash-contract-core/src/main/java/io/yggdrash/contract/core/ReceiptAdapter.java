package io.yggdrash.contract.core;

import java.util.List;
import java.util.Set;

public class ReceiptAdapter implements Receipt {

    Receipt tr;

    public void setReceipt(Receipt tr) {
        this.tr = tr;
    }

    @Override
    public void setIssuer(String issuer) {
        // adapter can not set issuer
    }

    @Override
    public void setBranchId(String branchId) {
        // adapter can not set branchId
    }

    @Override
    public void setBlockId(String blockId) {
        // adapter can not set blockId
    }

    @Override
    public void setBlockHeight(Long blockHeight) {
        // adapter can not set TxId
    }

    @Override
    public void setTxId(String txId) {
        // adapter can not set TxId
    }

    @Override
    public void setContractVersion(String contractId) {
        // adapter can not set contractVersion
    }

    @Override
    public void setMethod(String method) {
        // adapter can not set method
    }

    @Override
    public void addLog(String log) {
        this.tr.addLog(log);
    }

    @Override
    public void setStatus(ExecuteStatus status) {
        this.tr.setStatus(status);
    }

    @Override
    public void addEvent(ContractEvent event) {
        this.tr.addEvent(event);
    }

    @Override
    public String getIssuer() {
        return this.tr.getIssuer();
    }

    @Override
    public String getBranchId() {
        return this.tr.getBranchId();
    }

    @Override
    public String getBlockId() {
        return this.tr.getBlockId();
    }

    @Override
    public Long getBlockSize() {
        return this.tr.getBlockSize();
    }

    @Override
    public Long getBlockHeight() {
        return this.tr.getBlockHeight();
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
    public String getContractVersion() {
        return this.tr.getContractVersion();
    }

    @Override
    public String getMethod() {
        return this.tr.getMethod();
    }

    @Override
    public List<String> getLog() {
        return this.tr.getLog();
    }

    @Override
    public boolean isSuccess() {
        return this.tr.isSuccess();
    }

    @Override
    public ExecuteStatus getStatus() {
        return tr.getStatus();
    }

    @Override
    public Set<ContractEvent> getEvents() {
        return this.tr.getEvents();
    }

}
