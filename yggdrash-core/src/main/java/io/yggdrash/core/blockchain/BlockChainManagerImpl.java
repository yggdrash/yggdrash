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

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockChainManagerImpl<T> implements BlockChainManager<T> {
    private static final Logger log = LoggerFactory.getLogger(BlockChainImpl.class);

    private ConsensusBlock<T> lastConfirmedBlock;
    private final ConsensusBlockStore<T> blockStore;
    private final TransactionStore transactionStore;
    private final TransactionReceiptStore transactionReceiptStore;

    public BlockChainManagerImpl(ConsensusBlockStore<T> blockStore,
                                 TransactionStore transactionStore,
                                 TransactionReceiptStore transactionReceiptStore) {
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.transactionReceiptStore = transactionReceiptStore;
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
    public boolean verifyGenesis(Block block) {
        return VerifierUtils.verifyGenesis(block);
    }

    @Override
    public boolean verifyNewBlock(ConsensusBlock<T> nextBlock) {
        if (lastConfirmedBlock == null) {
            return true;
        }

        if (lastConfirmedBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", lastConfirmedBlock.getIndex(), nextBlock.getIndex());
            String msg = String.format("Invalid to chain cur=%s, new=%s",
                    lastConfirmedBlock.getIndex(), nextBlock.getIndex()); //duplicated code
            throw new NotValidateException(msg);
        } else if (!lastConfirmedBlock.getHash().equals(nextBlock.getPrevBlockHash())) {
            log.warn("invalid previous hash= {} {}", lastConfirmedBlock.getHash(), nextBlock.getPrevBlockHash());
            String msg = String.format("Invalid to chain cur=%s, new=%s",
                    lastConfirmedBlock.getIndex(), nextBlock.getIndex());
            throw new NotValidateException(msg);
        }

        return true;
    }

    @Override
    public ConsensusBlock<T> addBlock(ConsensusBlock<T> nextBlock) {
        if (nextBlock == null
                || (verifyGenesis(nextBlock) && nextBlock.getIndex() != getLastIndex() + 1)
                || !VerifierUtils.verify(nextBlock)) {
            log.debug("Block is not valid.");
            return null;
        }
        // Store Block Index and Block Data
        this.blockStore.addBlock(nextBlock);
        this.lastConfirmedBlock = nextBlock;

        batchTxs(nextBlock);

        return nextBlock;

    }

    private void batchTxs(ConsensusBlock<T> block) {
        if (block == null || block.getBlock() == null || block.getBody().getTransactionList() == null) {
            return;
        }

        Set<Sha3Hash> keys = block.getBlock().getBody().getTransactionList().stream()
                .map(Transaction::getHash).collect(Collectors.toSet());

        transactionStore.batch(keys);
    }

    @Override
    public Transaction addTransaction(Transaction tx) {
        if (transactionStore.contains(tx.getHash())) {
            return null;
        } else if (!VerifierUtils.verify(tx)) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    @Override
    public void updateTxCache(Block block) {
        transactionStore.updateCache(block.getBody().getTransactionList());
    }

    @Override
    public void setLastConfirmedBlock(ConsensusBlock<T> block) {
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

    @Override
    public List<Transaction> getUnconfirmedTxs() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    @Override
    public List<Transaction> getUnconfirmedTxsWithLimit(long limit) {
        return transactionStore.getUnconfirmedTxsWithLimit(limit);
    }

    @Override
    public TransactionReceipt getTransactionReceipt(String txId) {
        return transactionReceiptStore.get(txId);
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
    public long countOfTxs() {
        return transactionStore.countOfTxs();
    }

    @Override
    public boolean containsBlockHash(Sha3Hash blockHash) {
        return blockStore.contains(blockHash);
    }

    @Override
    public boolean containsTxHash(Sha3Hash txHash) {
        return transactionStore.contains(txHash);
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
        this.transactionReceiptStore.close();
    }
}
