/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.Contract;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BranchGroup {

    private Map<BranchId, BlockChain> branches = new ConcurrentHashMap<>();
    private static BlockChain chain;

    public void addBranch(BranchId branchId, BlockChain blockChain) {
        if (branches.containsKey(branchId)) {
            throw new DuplicatedException(branchId.toString());
        }
        chain = blockChain; // TODO remove
        branches.put(branchId, blockChain);
    }

    public BlockChain getBranch(BranchId branchId) {
        return branches.get(branchId);
    }

    public Collection<BlockChain> getAllBranch() {
        return branches.values();
    }

    public TransactionHusk addTransaction(TransactionHusk tx) {
        if (branches.containsKey(tx.getBranchId())) {
            return branches.get(tx.getBranchId()).addTransaction(tx);
        }
        return tx;
    }

    @Deprecated
    public long getLastIndex() {
        return getLastIndex(chain.getBranchId());
    }

    public long getLastIndex(BranchId id) {
        return branches.get(id).getLastIndex();
    }

    @Deprecated
    public List<TransactionHusk> getTransactionList() {
        return getTransactionList(chain.getBranchId());
    }

    public List<TransactionHusk> getTransactionList(BranchId branchId) {
        return branches.get(branchId).getTransactionList();
    }

    @Deprecated
    public TransactionHusk getTxByHash(String id) {
        return getTxByHash(chain.getBranchId(), new Sha3Hash(id));
    }

    public TransactionHusk getTxByHash(BranchId branchId, String id) {
        return getTxByHash(branchId, new Sha3Hash(id));
    }

    public TransactionHusk getTxByHash(BranchId branchId, Sha3Hash hash) {
        return branches.get(branchId).getTxByHash(hash);
    }

    public void generateBlock(Wallet wallet) {
        branches.values().forEach(chain -> chain.generateBlock(wallet));
    }

    public BlockHusk addBlock(BlockHusk block) {
        if (branches.containsKey(block.getBranchId())) {
            return chain.addBlock(block);
        }
        return block;
    }

    @Deprecated
    public BlockHusk getBlockByIndex(long index) {
        return getBlockByIndex(chain.getBranchId(), index);
    }

    public BlockHusk getBlockByIndex(BranchId branchId, long index) {
        return branches.get(branchId).getBlockByIndex(index);
    }

    @Deprecated
    public BlockHusk getBlockByHash(String hash) {
        return getBlockByHash(chain.getBranchId(), hash);
    }

    public BlockHusk getBlockByHash(BranchId branchId, String hash) {
        return branches.get(branchId).getBlockByHash(hash);
    }

    public int getBranchSize() {
        return branches.size();
    }

    @Deprecated
    public StateStore<?> getStateStore() {
        return getStateStore(chain.getBranchId());
    }

    public StateStore<?> getStateStore(BranchId branchId) {
        return branches.get(branchId).getRuntime().getStateStore();
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return chain.getRuntime().getTransactionReceiptStore();
    }

    @Deprecated
    public Contract getContract() {
        return getContract(chain.getBranchId());
    }

    public Contract getContract(BranchId branchId) {
        return branches.get(branchId).getContract();
    }

    @Deprecated
    public JsonObject query(JsonObject query) {
        return query(chain.getBranchId(), query);
    }

    public JsonObject query(BranchId branchId, JsonObject query) {
        try {
            BlockChain chain = branches.get(branchId);
            return chain.getRuntime().query(chain.getContract(), query);
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }
}
