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

package io.yggdrash.core.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.TempStateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Runtime {
    private static final Logger log = LoggerFactory.getLogger(Runtime.class);

    private final StateStore stateStore;
    private final TransactionReceiptStore txReceiptStore;
    private final Map<ContractVersion, RuntimeContractWrap> contracts = new HashMap<>();

    // All block chain has state root
    private byte[] stateRoot;


    // FIX runtime run contract will init
    // TODO Runtime get multi Contract
    public Runtime(StateStore stateStore, TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.txReceiptStore = txReceiptStore;
    }

    public Set<ContractVersion> executeAbleContract() {
        return this.contracts.keySet();
    }

    // TODO contract move to Map
    public void addContract(ContractVersion contractVersion, Contract contract) {
        RuntimeContractWrap wrap = new RuntimeContractWrap(contractVersion, contract);
        wrap.setStore(stateStore);
        this.contracts.put(contractVersion, wrap);
    }

    public boolean hasContract(ContractVersion contractVersion) {
        return this.contracts.containsKey(contractVersion);
    }


    public BlockRuntimeResult invokeBlock(Block block) {
        // Block Data
        // - Hash
        // - BranchId
        // - Index *(Height)
        // Map String

        if (block.getIndex() == 0) {
            // TODO first transaction is genesis
            // TODO init method don't call any more
        }

        BlockRuntimeResult result = new BlockRuntimeResult(block);
        TempStateStore blockState = new TempStateStore(stateStore);
        List<TransactionHusk> txList = block.getBody();
        for (TransactionHusk tx: txList) {
            TransactionReceipt txReceipt = ContractManager.createTransactionReceipt(tx);
            // set Block ID
            txReceipt.setBlockId(block.getHash().toString());
            txReceipt.setBlockHeight(block.getIndex());
            txReceipt.setBranchId(block.getBranchId().toString());

            // Transaction invoke here
            // save Transaction Receipt
            TempStateStore txResult = invoke(tx, txReceipt, blockState);
            if (txReceipt.isSuccess()) {
                blockState.putAll(txResult.changeValues());
            }
            log.debug("{} is {}", txReceipt.getTxId(), txReceipt.isSuccess());

            result.addTxReceipt(txReceipt);
            // Save TxReceipt
        }
        // Save BlockStates
        result.setBlockResult(blockState.changeValues());

        return result;
    }

    public void commitBlockResult(BlockRuntimeResult result) {
        // TODO store transaction by batch
        Map<String, JsonObject> changes = result.getBlockResult();
        result.getTxReceipts().forEach(txReceiptStore::put);
        if (!changes.isEmpty()) {
            changes.forEach(stateStore::put);
        }
        // TODO make transaction Receipt Event

    }

    // This invoke is temp run Transaction
    public TransactionRuntimeResult invoke(TransactionHusk tx) {
        TransactionReceipt txReceipt = ContractManager.createTransactionReceipt(tx);
        TransactionRuntimeResult trr = new TransactionRuntimeResult(tx);
        trr.setTransactionReceipt(txReceipt);

        TempStateStore store = invoke(tx, txReceipt, stateStore);
        trr.setChangeValues(store.changeValues());

        return trr;
    }

    private TempStateStore invoke(TransactionHusk tx, TransactionReceipt txReceipt, ReadWriterStore origin) {
        // Find invoke method and invoke
        // validation method
        TempStateStore txState = new TempStateStore(origin);
        try {
            // transaction is multiple method
            for (JsonElement transactionElement: JsonUtil.parseJsonArray(tx.getBody())) {
                JsonObject txBody = transactionElement.getAsJsonObject();
                // check contract Version
                String contractVersion = txBody.get("contractVersion").getAsString();
                ContractVersion txContractVersion = ContractVersion.ofNonHex(contractVersion);
                RuntimeContractWrap wrap = contracts.get(txContractVersion);
                // TODO remove this (retry if not system contract)
                if (wrap == null) {
                    txContractVersion = ContractVersion.of(txBody.get("contractVersion").getAsString());
                    wrap = contracts.get(txContractVersion);
                }
                log.debug("txContractVersion {}", txContractVersion);
                txReceipt.setContractVersion(txContractVersion.toString());
                TempStateStore txElementState = wrap.invokeTransaction(txBody, txReceipt, txState);
                if (txReceipt.isSuccess()) {
                    txState.putAll(txElementState.changeValues());
                }
                log.debug("invoke {} is {}", txReceipt.getTxId(), txReceipt.isSuccess());
            }

        } catch (Throwable e) {
            log.warn(e.getMessage());
            txReceipt.setStatus(ExecuteStatus.ERROR);
            JsonObject errorLog = new JsonObject();
            errorLog.addProperty("error", e.getMessage());
            txReceipt.addLog(errorLog.toString());
        }
        return txState;
    }

    public Object query(ContractVersion id, String method, JsonObject params) throws Exception {
        RuntimeContractWrap contractWrap = this.contracts.get(id);
        return contractWrap.query(method, params);
    }

    // TODO Remove This
    public StateStore getStateStore() {
        return this.stateStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.txReceiptStore;
    }

    public TransactionReceipt getTransactionReceipt(String txId) {
        return txReceiptStore.get(txId);
    }
}