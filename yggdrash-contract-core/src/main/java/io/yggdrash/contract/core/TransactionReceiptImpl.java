package io.yggdrash.contract.core;

import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;

import java.util.ArrayList;
import java.util.List;

public class TransactionReceiptImpl implements TransactionReceipt {
    private String txId;
    private String blockId;
    private String branchId;
    private final List<String> txLog = new ArrayList<>();
    private ExecuteStatus status = ExecuteStatus.FALSE;
    private String issuer;
    private String contractVersion;
    private Long blockHeight;
    private String methodName;

    public TransactionReceiptImpl() {
        //init;
    }

    public TransactionReceiptImpl(String txId, String issuer) {
        this.txId = txId;
        this.issuer = issuer;
    }

    public void addLog(String log) {
        txLog.add(log);
    }

    public ExecuteStatus getStatus() {
        return status;
    }

    public void setStatus(ExecuteStatus status) {
        this.status = status;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    @Override
    public String getContractVersion() {
        return contractVersion;
    }

    @Override
    public void setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
    }

    public List<String> getTxLog() {
        return txLog;
    }

    public String transactionMethod() {
        return methodName;
    }

    public void setTransactionMethod(String methodName) {
        this.methodName = methodName;
    }

    public boolean isSuccess() {
        return status == ExecuteStatus.SUCCESS;
    }

//    @Override
//    public String toString() {
//        return JsonUtil.convertObjToString(this);
//    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
