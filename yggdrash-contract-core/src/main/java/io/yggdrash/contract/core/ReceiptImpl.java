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
import java.util.Set;

public class ReceiptImpl implements Receipt {

    private String issuer;
    private String branchId;
    private String blockId;
    private Long blockSize;
    private Long blockHeight;
    private String txId;
    private Long txSize;
    private String contractVersion;
    private String method;
    private final List<String> log = new ArrayList<>();
    private ExecuteStatus status = ExecuteStatus.FALSE;
    private ContractEventSet event = new ContractEventSet();

    public ReceiptImpl() { //TODO check initialize variables
    }

    public ReceiptImpl(String issuer, String branchId, String blockId, Long blockSize, Long blockHeight) {
        this.issuer = issuer;
        this.branchId = branchId;
        this.blockId = blockId;
        this.blockSize = blockSize;
        this.blockHeight = blockHeight;
    }

    public ReceiptImpl(String txId, Long txSize, String issuer) {
        this.txId = txId;
        this.txSize = txSize;
        this.issuer = issuer;
    }

    public ReceiptImpl(String txId, Long txSize, String issuer, String contractVersion) {
        this.issuer = issuer;
        this.txId = txId;
        this.txSize = txSize;
        this.contractVersion = contractVersion;
    }

    @Override
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    @Override
    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    @Override
    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    @Override
    public void setTxId(String txId) {
        this.txId = txId;
    }

    @Override
    public void setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public void addLog(String msg) {
        log.add(msg);
    }

    @Override
    public void setStatus(ExecuteStatus status) {
        this.status = status;
    }

    @Override
    public void setEvent(ContractEventSet event) {
        this.event = event;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public String getBranchId() {
        return branchId;
    }

    @Override
    public String getBlockId() {
        return blockId;
    }

    @Override
    public Long getBlockSize() {
        return blockSize;
    }

    @Override
    public Long getBlockHeight() {
        return blockHeight;
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
    public String getContractVersion() {
        return contractVersion;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public List<String> getLog() {
        return log;
    }

    @Override
    public boolean isSuccess() {
        return status == ExecuteStatus.SUCCESS;
    }

    @Override
    public ExecuteStatus getStatus() {
        return status;
    }

    @Override
    public Set<ContractEvent> getEvents() {
        return event.getEvents();
    }

}
