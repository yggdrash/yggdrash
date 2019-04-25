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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.TransactionReceiptStore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BranchGroup {
    private final Map<BranchId, BlockChain> branches = new ConcurrentHashMap<>();

    public void addBranch(BlockChain blockChain) {
        if (blockChain == null) {
            return;
        }
        BranchId branchId = blockChain.getBranchId();
        if (branches.containsKey(branchId)) {
            throw new DuplicatedException(branchId.toString() + " duplicated");
        }
        branches.put(branchId, blockChain);
    }

    public BlockChain getBranch(BranchId branchId) {
        return branches.get(branchId);
    }

    private boolean containsBranch(BranchId branchId) {
        return branches.containsKey(branchId);
    }

    public Collection<BlockChain> getAllBranch() {
        return branches.values();
    }

    public Transaction addTransaction(Transaction tx) {
        if (branches.containsKey(tx.getBranchId())) {
            return branches.get(tx.getBranchId()).addTransaction(tx);
        }
        return tx;
    }

    public long getLastIndex(BranchId id) {
        if (branches.containsKey(id)) {
            return branches.get(id).getLastIndex();
        }
        return 0L;
    }

    public Collection<Transaction> getRecentTxs(BranchId branchId) {
        return branches.get(branchId).getRecentTxs();
    }

    public Transaction getTxByHash(BranchId branchId, String id) {
        return getTxByHash(branchId, new Sha3Hash(id));
    }

    Transaction getTxByHash(BranchId branchId, Sha3Hash hash) {
        return branches.get(branchId).getTxByHash(hash);
    }

    ConsensusBlock addBlock(ConsensusBlock block) {
        return addBlock(block, true);
    }

    public ConsensusBlock addBlock(ConsensusBlock block, boolean broadcast) {
        if (branches.containsKey(block.getBranchId())) {
            return branches.get(block.getBranchId()).addBlock(block, broadcast);
        }
        return block;
    }

    public ConsensusBlock getBlockByIndex(BranchId branchId, long index) {
        return branches.get(branchId).getBlockByIndex(index);
    }

    public ConsensusBlock getBlockByHash(BranchId branchId, String hash) {
        return branches.get(branchId).getBlockByHash(new Sha3Hash(hash));
    }

    int getBranchSize() {
        return branches.size();
    }

    public StateStore getStateStore(BranchId branchId) {
        return branches.get(branchId).getStateStore();
    }

    TransactionReceiptStore getTransactionReceiptStore(BranchId branchId) {
        return branches.get(branchId).getTransactionReceiptStore();
    }

    public TransactionReceipt getTransactionReceipt(BranchId branchId, String transactionId) {
        return branches.get(branchId).getTransactionReceipt(transactionId);
    }

    public List<Transaction> getUnconfirmedTxs(BranchId branchId) {
        if (branches.containsKey(branchId)) {
            return branches.get(branchId).getUnconfirmedTxs();
        } else {
            return Collections.emptyList();
        }
    }

    public Object query(BranchId branchId, String contractVersion, String method, JsonObject params) {
        if (!containsBranch(branchId)) {
            throw new NonExistObjectException(branchId.toString() + " branch");
        }
        try {
            BlockChain chain = branches.get(branchId);
            return chain.getContractManager().query(contractVersion, method, params);
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    public long countOfTxs(BranchId branchId) {
        return branches.get(branchId).countOfTxs();
    }
}
