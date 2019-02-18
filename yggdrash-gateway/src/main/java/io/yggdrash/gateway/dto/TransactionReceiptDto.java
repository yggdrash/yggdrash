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

package io.yggdrash.gateway.dto;

import io.yggdrash.core.contract.TransactionReceipt;
import java.util.ArrayList;
import java.util.List;

public class TransactionReceiptDto {

    public String txId;
    public String blockId;
    public String branchId;
    public List<String> txLog = new ArrayList<>();
    public String status;
    public String issuer;
    public String contractVersion;
    public Long blockHeight;
    public String methodName;

    public static TransactionReceiptDto createBy(TransactionReceipt tx) {
        TransactionReceiptDto transactionDto = new TransactionReceiptDto();
        transactionDto.txLog = tx.getTxLog();
        transactionDto.txId = tx.getTxId();
        transactionDto.blockId = tx.getBlockId();
        transactionDto.branchId = tx.getBranchId();
        transactionDto.status = tx.getStatus().toString();
        transactionDto.issuer = tx.getIssuer();
        transactionDto.contractVersion = tx.getContractVersion();
        transactionDto.blockHeight = tx.getBlockHeight();
        // TODO Add method Name

        return transactionDto;
    }
}
