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
import io.yggdrash.core.contract.ContractVersion;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.wallet.Wallet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public Set<BranchId> getAllBranchId() {
        return branches.keySet();
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

    public long getLastIndex(BranchId id) {
        if (branches.containsKey(id)) {
            return branches.get(id).getLastIndex();
        }
        return 0L;
    }

    public Collection<TransactionHusk> getRecentTxs(BranchId branchId) {
        return branches.get(branchId).getRecentTxs();
    }

    public TransactionHusk getTxByHash(BranchId branchId, String id) {
        return getTxByHash(branchId, new Sha3Hash(id));
    }

    TransactionHusk getTxByHash(BranchId branchId, Sha3Hash hash) {
        return branches.get(branchId).getTxByHash(hash);
    }

    public void generateBlock(Wallet wallet, BranchId branchId) {
        branches.get(branchId).generateBlock(wallet);
    }

    BlockHusk addBlock(BlockHusk block) {
        return addBlock(block, true);
    }

    public BlockHusk addBlock(BlockHusk block, boolean broadcast) {
        if (branches.containsKey(block.getBranchId())) {
            return branches.get(block.getBranchId()).addBlock(block, broadcast);
        }
        return block;
    }

    public BlockHusk getBlockByIndex(BranchId branchId, long index) {
        return branches.get(branchId).getBlockByIndex(index);
    }

    public BlockHusk getBlockByHash(BranchId branchId, String hash) {
        return branches.get(branchId).getBlockByHash(hash);
    }

    int getBranchSize() {
        return branches.size();
    }

    public StateStore<?> getStateStore(BranchId branchId) {
        return branches.get(branchId).getRuntime().getStateStore();
    }

    public TransactionReceiptStore getTransactionReceiptStore(BranchId branchId) {
        return branches.get(branchId).getRuntime().getTransactionReceiptStore();
    }

    public TransactionReceipt getTransactionReceipt(BranchId branchId, String transactionId) {
        return branches.get(branchId).getRuntime().getTransactionReceipt(transactionId);
    }


    public List<TransactionHusk> getUnconfirmedTxs(BranchId branchId) {
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
            // TODO change branch spec
            // get runtime contract ID and execute


//            ContractVersion version = ContractVersion.of(contractVersion);
            ContractVersion version = ContractVersion.ofNonHex(contractVersion);
            return chain.getRuntime().query(version, method, params);
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    public long countOfTxs(BranchId branchId) {
        return branches.get(branchId).countOfTxs();
    }

    private boolean isHexString(String version) {
        String hexVersion = "0x" + version;
        for(int i = 0; i < hexVersion.length(); i++) {
            if(Character.digit(hexVersion.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }
}
