/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract.core;

import java.util.ArrayList;
import java.util.List;

public class TransactionReceiptImpl implements TransactionReceipt {

    private String txId;
    private Long txSize;
    private String blockId;
    private String branchId;
    private final List<String> txLog = new ArrayList<>();
    private ExecuteStatus status = ExecuteStatus.FALSE;
    private String issuer;
    private String contractVersion;
    private Long blockHeight;
    private String methodName;

    public TransactionReceiptImpl() {
    }

    public TransactionReceiptImpl(String txId, Long txSize, String issuer) {
        this.txId = txId;
        this.txSize = txSize;
        this.setIssuer(issuer);
    }

    public TransactionReceiptImpl(String txId, Long txSize, String issuer, String contractVersion) {
        this.txId = txId;
        this.txSize = txSize;
        this.contractVersion = contractVersion;
        this.setIssuer(issuer);
    }

    @Override
    public void addLog(String log) {
        txLog.add(log);
    }

    @Override
    public ExecuteStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ExecuteStatus status) {
        this.status = status;
    }

    @Override
    public String getTxId() {
        return txId;
    }

    @Override
    public Long getTxSize() {
        return txSize;
    }

    @Override
    public void setTxId(String txId) {
        this.txId = txId;
    }

    @Override
    public String getBlockId() {
        return blockId;
    }

    @Override
    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    @Override
    public Long getBlockHeight() {
        return blockHeight;
    }

    @Override
    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    @Override
    public String getBranchId() {
        return branchId;
    }

    @Override
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

    @Override
    public List<String> getTxLog() {
        return txLog;
    }

    @Override
    public boolean isSuccess() {
        return status == ExecuteStatus.SUCCESS;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public void setIssuer(String issuer) {
        this.issuer = issuer.toLowerCase();
    }

    public String transactionMethod() {
        return methodName;
    }

    public void setTransactionMethod(String methodName) {
        this.methodName = methodName;
    }
}
