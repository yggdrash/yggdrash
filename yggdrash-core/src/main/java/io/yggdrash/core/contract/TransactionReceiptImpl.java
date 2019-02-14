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

package io.yggdrash.core.contract;

import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.TransactionHusk;
import java.util.ArrayList;
import java.util.List;

public class TransactionReceiptImpl implements TransactionReceipt {

    private String txId;
    private String blockId;
    private String branchId;
    private final List<String> txLog = new ArrayList<>();
    private ExecuteStatus status = ExecuteStatus.FALSE;
    private String issuer;
    private String contractId;
    private Long blockHeight;
    private String methodName;

    public TransactionReceiptImpl() {
        //init;
    }

    public TransactionReceiptImpl(TransactionHusk tx) {
        this.txId = tx.getHash().toString();
        if (tx.getAddress() != null) {
            this.issuer = tx.getAddress().toString();
        }
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
        return contractId;
    }

    @Override
    public void setContractId(String contractId) {
        this.contractId = contractId;
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

    @Override
    public String toString() {
        return JsonUtil.convertObjToString(this);
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

}
