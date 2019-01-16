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

import com.google.gson.JsonObject;
import java.util.List;

public interface TransactionReceipt {

    void addLog(JsonObject log);

    ExecuteStatus getStatus();

    void setStatus(ExecuteStatus status);

    String getTxId();

    void setTxId(String txId);

    String getBlockId();

    void setBlockId(String blockId);

    Long getBlockHeight();

    void setBlockHeight(Long blockHeight);

    String getBranchId();

    void setBranchId(String branchId);

    String getContractId();

    void setContractId(String contractId);

    List<JsonObject> getTxLog();

    boolean isSuccess();

    String getIssuer();

    void setIssuer(String issuer);

}
