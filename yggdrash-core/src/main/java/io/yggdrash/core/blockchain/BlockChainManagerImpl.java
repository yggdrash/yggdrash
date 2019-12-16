/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.blockchain;

import com.google.common.collect.Lists;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.ReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BlockChainManagerImpl<T> implements BlockChainManager<T> {

    private static final Logger log = LoggerFactory.getLogger(BlockChainManagerImpl.class);

    private final BranchStore branchStore;
    private final ConsensusBlockStore<T> blockStore;
    private final TransactionStore transactionStore;
    private final ReceiptStore receiptStore;
    private final BlockChainStore blockChainStore;
    private final ReentrantLock lock = new ReentrantLock();

    private ConsensusBlock<T> lastConfirmedBlock;

    public BlockChainManagerImpl(BlockChainStore blockChainStore) {
        this.branchStore = blockChainStore.getBranchStore();
        this.blockStore = blockChainStore.getConsensusBlockStore();
        this.transactionStore = blockChainStore.getTransactionStore();
        this.receiptStore = blockChainStore.getReceiptStore();
        this.blockChainStore = blockChainStore;
    }

    @Override
    public void initGenesis(Block genesisBlock) {
        for (Transaction tx : genesisBlock.getBody().getTransactionList()) {
            if (!transactionStore.contains(tx.getHash())) {
                transactionStore.put(tx.getHash(), tx);
            }
        }
    }

    @Override
    public void loadTransaction() {
        try {
            lock.lock();
            // load recent 1000 block
            // Start Block and End Block
            long bestBlock = branchStore.getBestBlock();
            long loadStart = bestBlock > 1000 ? bestBlock - 1000 : 0;
            for (long i = loadStart; i <= bestBlock; i++) { // TODO Consider block verification (genesis, prevHash ...)
                // Load recent block and update cache
                ConsensusBlock<T> block = getBlockByIndex(i);
                // TODO Node can be shutdown before blockStore.addBlock()
                // addBlock(): branchStore.setBestBlock() -> executeTransactions() -> blockStore.addBlock()
                if (block == null) {
                    long prevIdx = i - 1;
                    ConsensusBlock<T> curBestBlock = getBlockByIndex(prevIdx);
                    branchStore.setBestBlock(curBestBlock);
                    //Set lastConfirmedBlock to the changed bestBlock. Currently, the lastConfirmedBlock is null
                    setLastConfirmedBlock(curBestBlock);
                    log.warn("Reset branchStore bestBlock: {} -> {}, lastExecutedBlock: {}",
                            bestBlock, prevIdx, branchStore.getLastExecuteBlockIndex());
                    break;
                }
                updateTxCache(block);
                // Set Last Best Block
                if (i == bestBlock) {
                    setLastConfirmedBlock(block);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int verify(Transaction transaction) {
        return verify(transaction, true);
    }

    private int verify(Transaction transaction, boolean isTxBroadcast) {
        int check = 0;

        check |= BusinessError.addCode(verifyDuplicated(transaction), BusinessError.DUPLICATED); // TODO Remove from verification

        if (isTxBroadcast) {
            check |= BusinessError.addCode(VerifierUtils.verifyTimestamp(transaction), BusinessError.REQUEST_TIMEOUT);
        }

        check |= BusinessError.addCode(VerifierUtils.verifyDataFormat(transaction), BusinessError.INVALID_DATA_FORMAT);

        check |= BusinessError.addCode(VerifierUtils.verifySignature(transaction), BusinessError.UNTRUSTED);

        return check;
    }

    @Override
    public int verify(ConsensusBlock<T> block) {
        int check = BusinessError.VALID.toValue();

        //GenesisBlock skips the newBlock verification
        if (lastConfirmedBlock != null) {
            if (!verifyBlockHeight(block)) {
                return BusinessError.UNKNOWN_BLOCK_HEIGHT.toValue(); // Immediate return if invalid blockHeight
            }

            check |= verifyNewBlock(block);
        }

        check |= BusinessError.addCode(verifyDuplicated(block), BusinessError.DUPLICATED);

        check |= BusinessError.addCode(VerifierUtils.verifyDataFormat(block), BusinessError.INVALID_DATA_FORMAT);

        check |= BusinessError.addCode(
                VerifierUtils.verifyBlockBodyHash(block), BusinessError.INVALID_MERKLE_ROOT_HASH);

        //TODO Verify txs of block

        return check;
    }

    private int verifyNewBlock(ConsensusBlock<T> nextBlock) {
        int check = BusinessError.VALID.toValue();

        check |= BusinessError.addCode(verifyBlockHash(nextBlock), BusinessError.INVALID_BLOCK_HASH);

        check |= BusinessError.addCode(VerifierUtils.verifySignature(nextBlock), BusinessError.UNTRUSTED);

        return check;
    }

    private boolean verifyDuplicated(Transaction transaction) {
        return !transactionStore.contains(transaction.getHash());
    }

    private boolean verifyDuplicated(Block block) {
        return !blockStore.contains(block.getHash());
    }

    private boolean verifyBlockHeight(ConsensusBlock<T> nextBlock) {
        return lastConfirmedBlock.getIndex() + 1 == nextBlock.getIndex();
    }

    private boolean verifyBlockHash(ConsensusBlock<T> nextBlock) {
        return lastConfirmedBlock.getHash().equals(nextBlock.getPrevBlockHash());
    }

    @Override
    public void addBlock(ConsensusBlock<T> nextBlock) {
        try {
            lock.lock();
            // A block may contain txs not received by txApi and those txs also have to be stored in the storage
            for (Transaction tx : nextBlock.getBody().getTransactionList()) {
                if (receiptStore.contains(tx.getHash().toString())
                        && receiptStore.get(tx.getHash().toString()).getStatus() != ExecuteStatus.ERROR) {
                    addTransaction(tx);
                }
            }
            batchTxs(nextBlock);

            // Store Block Index and Block Data
            this.blockStore.addBlock(nextBlock);
            setLastConfirmedBlock(nextBlock);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void batchTxs(ConsensusBlock<T> block) {
        try {
            lock.lock();
            if (block == null || block.getBlock() == null || block.getBody().getTransactionList() == null) {
                return;
            }

            Set<Sha3Hash> keys = block.getBlock().getBody().getTransactionList().stream()
                    .map(Transaction::getHash).collect(Collectors.toSet());
            transactionStore.batch(keys);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTransaction(Transaction tx) {
        try {
            transactionStore.addTransaction(tx);
            log.trace("AddTransaction: txStore added tx={}", tx.getHash().toString());
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    @Override
    public void flushUnconfirmedTxs(Set<Sha3Hash> keys) {
        transactionStore.flush(keys);
    }

    @Override
    public void updateTxCache(Block block) {
        transactionStore.updateCache(block);
    }

    private void setLastConfirmedBlock(ConsensusBlock<T> block) {
        this.lastConfirmedBlock = block;
    }

    @Override
    public ConsensusBlock<T> getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     **/
    @Override
    public ConsensusBlock<T> getBlockByHash(Sha3Hash hash) {
        return blockStore.get(hash);
    }

    @Override
    public ConsensusBlock<T> getBlockByIndex(long index) {
        try {
            return blockStore.getBlockByIndex(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     **/
    @Override
    public Transaction getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    @Override
    public Collection<Transaction> getRecentTxs() {
        return transactionStore.getRecentTxs();
    }

    /*
    @Override
    public void setPendingStateRoot(Sha3Hash stateRootHash) {
        transactionStore.setStateRoot(stateRootHash);
    }
    */

    @Override
    public List<Transaction> getUnconfirmedTxs() {
        // TODO change queue List
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    @Override
    public int getUnconfirmedTxSize() {
        return transactionStore.getUnconfirmedTxsSize();
    }

    public List<Transaction> filtered(List<Transaction> txList) {
        // 1 txList to Set

        HashMap<Sha3Hash, Transaction> filteredMap = new HashMap<>();

        for (Transaction tx : txList) {
            filteredMap.put(tx.getHash(), tx);
        }

        Set<Sha3Hash> filterSet = filteredMap.keySet();

        // 2 Set to check contain
        for (Sha3Hash txHash : filterSet) {
            if (transactionStore.contains(txHash)) {
                filteredMap.remove(txHash);
            }
        }

        // 3 Set to List
        return new ArrayList<>(filteredMap.values());
    }


    @Override
    public List<Transaction> getUnconfirmedTxsWithLimit(long limit) {
        return transactionStore.getUnconfirmedTxsWithLimit(limit);
    }

    @Override
    public Receipt getReceipt(String key) {
        return receiptStore.get(key);
    }

    @Override
    public Sha3Hash getLastHash() {
        return lastConfirmedBlock.getHash();
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     **/
    @Override
    public long getLastIndex() {
        return lastConfirmedBlock == null ? 0 : lastConfirmedBlock.getIndex();
    }

    @Override
    public long countOfBlocks() {
        return blockStore.size();
    }

    @Override
    public boolean containsBlockHash(Sha3Hash blockHash) {
        return blockStore.contains(blockHash);
    }

    @Override
    public boolean contains(Block block) {
        return blockStore.contains(block.getHash());
    }

    @Override
    public boolean contains(Transaction transaction) {
        return transactionStore.contains(transaction.getHash());
    }

    @Override
    public void close() {
        this.blockStore.close();
        this.transactionStore.close();
        this.receiptStore.close();
    }
}
