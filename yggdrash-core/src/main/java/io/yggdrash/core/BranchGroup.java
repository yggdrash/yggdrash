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

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.event.BranchEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BranchGroup {

    private Map<BranchId, BlockChain> branches = new ConcurrentHashMap<>();
    private static BlockChain chain;

    private BranchEventListener listener;

    public void addBranch(BranchId branchId, BlockChain blockChain) {
        if (branches.containsKey(branchId)) {
            return;
        }
        chain = blockChain; // TODO remove
        branches.put(branchId, blockChain);
    }

    public void setListener(BranchEventListener listener) {
        this.listener = listener;
    }

    public TransactionHusk addTransaction(TransactionHusk tx) {
        return chain.addTransaction(tx);
    }

    public long getLastIndex() {
        return chain.getLastIndex();
    }

    public List<TransactionHusk> getTransactionList() {
        return chain.getTransactionList();
    }

    public TransactionHusk getTxByHash(String id) {
        return getTxByHash(new Sha3Hash(id));
    }

    public TransactionHusk getTxByHash(Sha3Hash hash) {
        return chain.getTxByHash(hash);
    }

    public BlockHusk generateBlock(Wallet wallet) {
        BlockHusk newBlock = chain.generateBlock(wallet);
        if (listener != null && newBlock != null) {
            listener.chainedBlock(newBlock);
        }
        return newBlock;
    }

    public BlockHusk addBlock(BlockHusk block) {
        return chain.addBlock(block);
    }

    public Set<BlockHusk> getBlocks() {
        return chain.getBlocks();
    }

    public BlockHusk getBlockByIndexOrHash(String indexOrHash) {
        if (isNumeric(indexOrHash)) {
            int index = Integer.parseInt(indexOrHash);
            return chain.getBlockByIndex(index);
        } else {
            return chain.getBlockByHash(indexOrHash);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
