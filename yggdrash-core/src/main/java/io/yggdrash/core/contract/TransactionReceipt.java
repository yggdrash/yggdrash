/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import java.util.HashMap;
import java.util.Map;

public class TransactionReceipt {
    public static final int FALSE = 0;
    public static final int SUCCESS = 1;

    private String transactionHash;
    private String blockHash;
    private final int yeedUsed = 30000;
    private String branchAddress;
    private final Map<String, Object> txLog = new HashMap<>();
    private int status = FALSE;

    public void putLog(String key, Object value) {
        txLog.put(key, value);
    }

    public Object getLog(String key) {
        return txLog.get(key);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public int getYeedUsed() {
        return yeedUsed;
    }

    public String getBranchAddress() {
        return branchAddress;
    }

    public Map<String, Object> getTxLog() {
        return txLog;
    }

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == SUCCESS;
    }

    public static TransactionReceipt errorReceipt(String transactionHash, Throwable e) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setTransactionHash(transactionHash);
        txReceipt.setStatus(TransactionReceipt.FALSE);
        txReceipt.putLog("Error", e);
        return txReceipt;
    }

    @Override
    public String toString() {
        return "TransactionReceipt{"
                + "transactionHash='" + transactionHash + '\''
                + ", blockHash='" + blockHash + '\''
                + ", yeedUsed=" + yeedUsed
                + ", branchAddress='" + branchAddress + '\''
                + ", txLog=" + txLog
                + ", status=" + status
                + '}';
    }
}
