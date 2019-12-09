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
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.DecodeException;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.errorcode.SystemError;

import java.util.Collection;
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
        if (!isBranchExist(branchId)) {
            throw new NonExistObjectException.BranchNotFound(branchId.toString());
        }
        return branches.get(branchId);
    }

    public boolean isFullSynced(BranchId branchId) {
        return getBranch(branchId).isFullSynced();
    }

    public int verifyBranchContractExist(BranchId branchId, String contractVersion) {
        return verifyBranchContractExist(branchId, ContractVersion.of(contractVersion));
    }

    private int verifyBranchContractExist(BranchId branchId, ContractVersion contractVersion) {
        int check = 0;

        check |= SystemError.addCode(isBranchExist(branchId), SystemError.BRANCH_NOT_FOUND);
        if (isBranchExist(branchId) && !contractVersion.equals(ContractConstants.VERSIONING_CONTRACT)) {
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
        String version = !tx.getTransactionBody().getBody().has("contractVersion")
                || (!tx.getTransactionBody().getBody().get("contractVersion").isJsonPrimitive()
                || !tx.getTransactionBody().getBody().get("contractVersion").getAsJsonPrimitive().isString())
                ? "" : tx.getTransactionBody().getBody().get("contractVersion").getAsString();
        // Check Branch and Contract Exist
        int verifyResult = verifyBranchContractExist(tx.getBranchId(), version);
        if (verifyResult == SystemError.VALID.toValue()) {
            // Check Transaction Contents Verify
            return branches.get(tx.getBranchId()).addTransaction(tx);
        }
        return SystemError.getErrorLogsMap(verifyResult);
    }


    public long getLastIndex(BranchId branchId) {
        try {
            return getBranch(branchId).getBlockChainManager().getLastIndex();
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean isBranchExist(BranchId branchId) {
        return branches.containsKey(branchId);
    }

    public Collection<Transaction> getRecentTxs(BranchId branchId) {
        return getBranch(branchId).getBlockChainManager().getRecentTxs();
    }

    public Transaction getTxByHash(BranchId branchId, String id) {
        try {
            Sha3Hash txId = new Sha3Hash(id);
            return getTxByHash(branchId, txId);
        } catch (NonExistObjectException ne) {
            throw new NonExistObjectException();
        } catch (Exception e) {
            throw new DecodeException.TxIdNotHexString();
        }
    }

    Transaction getTxByHash(BranchId branchId, Sha3Hash hash) {
        return getBranch(branchId).getBlockChainManager().getTxByHash(hash);
    }

    void addBlock(ConsensusBlock block) {
        addBlock(block, true);
    }

    public void addBlock(ConsensusBlock block, boolean broadcast) {
        if (getBranch(block.getBranchId()).addBlock(block, broadcast).size() > 0) {
            throw new InternalErrorException.AddBlockFailed(block.getHash().toString());
        }
    }

    public ConsensusBlock getBlockByIndex(BranchId branchId, long index) {
        ConsensusBlock blockByIndex = getBranch(branchId).getBlockChainManager().getBlockByIndex(index);
        if (blockByIndex == null) {
            throw new NonExistObjectException.BlockNotFound();
        }
        return blockByIndex;
    }

    public ConsensusBlock getBlockByHash(BranchId branchId, String hash) {
        try {
            Sha3Hash blockHash = new Sha3Hash(hash);
            ConsensusBlock blockByHash = getBranch(branchId).getBlockChainManager().getBlockByHash(blockHash);
            if (blockByHash == null) {
                throw new NonExistObjectException.BlockNotFound(hash);
            }
            return blockByHash;
        } catch (NonExistObjectException ne) {
            throw new NonExistObjectException();
        } catch (Exception e) {
            throw new DecodeException.BlockIdNotHexString();
        }
    }

    int getBranchSize() {
        return branches.size();
    }

    public Receipt getReceipt(BranchId branchId, String key) {
        return getBranch(branchId).getBlockChainManager().getReceipt(key);
    }

    public List<Transaction> getUnconfirmedTxs(BranchId branchId) {
        return getBranch(branchId).getBlockChainManager().getUnconfirmedTxs();
    }

    public Object query(BranchId branchId, String contractVersion, String method, JsonObject params) {
        BlockChain chain = getBranch(branchId);
        try {
            return chain.getContractManager().query(contractVersion, method, params);
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

}
