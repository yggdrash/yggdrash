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
import java.util.HashMap;
import java.util.Map;

public class TransactionReceiptImpl implements TransactionReceipt {
    public static final int FALSE = 0;
    public static final int SUCCESS = 1;

    private String txId;
    private String blockId;
    private String branchId;
    private final Map<String, Object> txLog = new HashMap<>();
    private ExecuteStatus status = ExecuteStatus.FALSE;
    private String issuer;
    private Long blockHeight;

    public Object getLog(String key) {
        return txLog.get(key);
    }

    public void putLog(String key, Object value) {
        txLog.put(key, value);
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

    public Map<String, Object> getTxLog() {
        return txLog;
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
