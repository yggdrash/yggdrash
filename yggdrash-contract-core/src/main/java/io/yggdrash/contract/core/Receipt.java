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

package io.yggdrash.contract.core;

import java.util.List;
import java.util.Set;

public interface Receipt {

    void setIssuer(String issuer);

    void setBranchId(String branchId);

    void setBlockId(String blockId);

    void setBlockHeight(Long blockHeight);

    void setTxId(String txId);

    void setContractVersion(String contractId);

    void setMethod(String method);

    void addLog(String log);

    void setStatus(ExecuteStatus status);

    void setEvent(ContractEventSet event);

    String getIssuer();

    String getBranchId();

    String getBlockId();

    Long getBlockSize();

    Long getBlockHeight();

    Long getTxSize();

    String getTxId();

    String getContractVersion();

    String getMethod();

    List<String> getLog();

    boolean isSuccess();

    ExecuteStatus getStatus();

    Set<ContractEvent> getEvents();

}