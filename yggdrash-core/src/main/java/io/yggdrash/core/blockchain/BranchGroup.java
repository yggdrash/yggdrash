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
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.errorcode.SystemError;

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

    public int verify(BranchId branchId, String contractVersion) {
        return verify(branchId, ContractVersion.of(contractVersion));
    }

    private int verify(BranchId branchId, ContractVersion contractVersion) {
        int check = 0;

        check |= SystemError.addCode(containsBranch(branchId), SystemError.BRANCH_NOT_FOUND);
        if (containsBranch(branchId)) {
            check |= SystemError.addCode(
                    getBranch(branchId).containBranchContract(contractVersion),
                    SystemError.CONTRACT_VERSION_NOT_FOUND);
        }

        return check;
    }


    public Collection<BlockChain> getAllBranch() {
        return branches.values();
    }

    public Map<String, List<String>> addTransaction(Transaction tx) {
        // TxBody format has not been fixed yet. The following validation is required until the TxBody is fixed.
        String version = !tx.getBody().getBody().has("contractVersion")
                || (!tx.getBody().getBody().get("contractVersion").isJsonPrimitive()
                || !tx.getBody().getBody().get("contractVersion").getAsJsonPrimitive().isString())
                ? "" : tx.getBody().getBody().get("contractVersion").getAsString();

        int verifyResult = verify(tx.getBranchId(), version);
        if (verifyResult == SystemError.VALID.toValue()) {
            return branches.get(tx.getBranchId()).addTransaction(tx);
        }
        return SystemError.getErrorLogsMap(verifyResult);
    }

    public long getLastIndex(BranchId id) {
        if (branches.containsKey(id)) {
            return branches.get(id).getBlockChainManager().getLastIndex();
        }
        return 0L;
    }

    public Collection<Transaction> getRecentTxs(BranchId branchId) {
        return branches.get(branchId).getBlockChainManager().getRecentTxs();
    }

    public Transaction getTxByHash(BranchId branchId, String id) {
        return getTxByHash(branchId, new Sha3Hash(id));
    }

    Transaction getTxByHash(BranchId branchId, Sha3Hash hash) {
        return branches.get(branchId).getBlockChainManager().getTxByHash(hash);
    }

    Map<String, List<String>> addBlock(ConsensusBlock block) {
        return addBlock(block, true);
    }

    public Map<String, List<String>> addBlock(ConsensusBlock block, boolean broadcast) {
        if (branches.containsKey(block.getBranchId())) {
            return branches.get(block.getBranchId()).addBlock(block, broadcast);
        }
        return SystemError.getErrorLogsMap(SystemError.BRANCH_NOT_FOUND.toValue());
    }

    public ConsensusBlock getBlockByIndex(BranchId branchId, long index) {
        return branches.get(branchId).getBlockChainManager().getBlockByIndex(index);
    }

    public ConsensusBlock getBlockByHash(BranchId branchId, String hash) {
        return branches.get(branchId).getBlockChainManager().getBlockByHash(new Sha3Hash(hash));
    }

    int getBranchSize() {
        return branches.size();
    }

    public TransactionReceipt getTransactionReceipt(BranchId branchId, String transactionId) {
        return branches.get(branchId).getBlockChainManager().getTransactionReceipt(transactionId);
    }

    public List<Transaction> getUnconfirmedTxs(BranchId branchId) {
        if (branches.containsKey(branchId)) {
            return branches.get(branchId).getBlockChainManager().getUnconfirmedTxs();
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
        return branches.get(branchId).getBlockChainManager().countOfTxs();
    }
}
