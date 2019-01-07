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

import io.yggdrash.common.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

// TODO TransactionReceipt to interface
public class TransactionReceipt {
    public static final int FALSE = 0;
    public static final int SUCCESS = 1;

    private String txId;
    private String blockId;
    private String branchId;
    private final Map<String, Object> txLog = new HashMap<>();
    private int status = FALSE;
    private String issuer;

    public void putLog(String key, Object value) {
        txLog.put(key, value);
    }

    public Object getLog(String key) {
        return txLog.get(key);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getTxId() {
        return txId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getBranchId() {
        return branchId;
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

    public static TransactionReceipt errorReceipt(String txId, Throwable e) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setTxId(txId);
        txReceipt.setStatus(TransactionReceipt.FALSE);
        txReceipt.putLog("Error", e);
        return txReceipt;
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
