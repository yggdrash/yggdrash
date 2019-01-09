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

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.wallet.Wallet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private final Branch branch;
    private final BlockHusk genesisBlock;
    private final List<BranchEventListener> listenerList = new ArrayList<>();

    private final BlockStore blockStore;
    private final TransactionStore transactionStore;
    private final MetaStore metaStore;
    private final StateStore stateStore;
    private final TransactionReceiptStore transactionReceiptStore;

    private final Contract contract;
    private final Runtime<?> runtime;

    private BlockHusk prevBlock;
    private final Map<Long, Sha3Hash> blockIndex = new HashMap<>();

    public BlockChain(Branch branch, BlockHusk genesisBlock, BlockStore blockStore,
                      TransactionStore transactionStore, MetaStore metaStore,
                      Contract contract, Runtime runtime) {
        this.branch = branch;
        this.genesisBlock = genesisBlock;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.metaStore = metaStore;
        this.contract = contract;
        this.runtime = runtime;
        this.stateStore = runtime.getStateStore();
        this.transactionReceiptStore = runtime.getTransactionReceiptStore();

        // Empty blockChain
        if (!blockStore.contains(genesisBlock.getHash())) {
            initGenesis();
        } else {
            indexing();
            loadTransaction();
        }
    }

    private void initGenesis() {
        for (TransactionHusk tx : genesisBlock.getBody()) {
            addTransaction(tx);
        }
        addBlock(genesisBlock, false);
    }

    private void indexing() {
        Sha3Hash storedBestBlockHash = metaStore.getBestBlockHash();
        Sha3Hash previousBlockHash = storedBestBlockHash;
        BlockHusk currentBlock;

        while (!BlockHusk.EMPTY_HASH.equals(previousBlockHash)) {
            currentBlock = blockStore.get(previousBlockHash);
            blockIndex.put(currentBlock.getIndex(), currentBlock.getHash());
            previousBlockHash = currentBlock.getPrevHash();
        }

        this.prevBlock = blockStore.get(storedBestBlockHash);
    }

    private void loadTransaction() {
        // Start Block and End Block
        Long bestblock = metaStore.getBestBlock();
        if (blockIndex.size() > bestblock) {
            for (long i = bestblock+1; i < blockIndex.size(); i++) {
                BlockHusk block = blockStore.get(blockIndex.get(i));
                // Genesis or check prevBlockHash
                if(block.getIndex() == 0 || block.getPrevHash().equals(metaStore.getBestBlockHash())) {
                    transactionStore.updateCache(block.getBody());
                    executeBlock(block);
                    log.debug("Load idx=[{}], tx={}, branch={}, blockHash={}",
                            blockStore.get(blockIndex.get(i)).getIndex(),
                            block.getBody().size(),
                            blockStore.get(blockIndex.get(i)).getBranchId(),
                            blockStore.get(blockIndex.get(i)).getHash());
                    // save best block
                    metaStore.setBestBlock(block);
                } else {
                    // prev Block hash is not equal
                    // so do not run any transactions
                }
            }
        }

    }

    public void addListener(BranchEventListener listener) {
        listenerList.add(listener);
    }

    public Contract getContract() {
        return contract;
    }

    Runtime<?> getRuntime() {
        return runtime;
    }

    void generateBlock(Wallet wallet) {
        List<TransactionHusk> txs = getUnconfirmedTxs();
        BlockHusk block = new BlockHusk(wallet, txs, getPrevBlock());
        addBlock(block, true);
    }

    Collection<TransactionHusk> getRecentTxs() {
        return transactionStore.getRecentTxs();
    }

    List<TransactionHusk> getUnconfirmedTxs() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    long countOfTxs() {
        return transactionStore.countOfTxs();
    }

    public Branch getBranch() {
        return branch;
    }

    public BranchId getBranchId() {
        return branch.getBranchId();
    }

    BlockHusk getGenesisBlock() {
        return this.genesisBlock;
    }

    BlockHusk getPrevBlock() {
        return this.prevBlock;
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     */
    public long getLastIndex() {
        if (isGenesisBlockChain()) {
            return 0;
        }
        return prevBlock.getIndex();
    }

    /**
     * Add block.
     *
     * @param nextBlock the next block
     * @throws NotValidateException the not validate exception
     */
    public BlockHusk addBlock(BlockHusk nextBlock, boolean broadcast) {
        if (blockStore.contains(nextBlock.getHash())) {
            return null;
        }
        if (!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidateException("Invalid to chain");
        }
        // run Block Transactions

        if (nextBlock.getIndex() > metaStore.getBestBlock()) {
            executeBlock(nextBlock);
            this.metaStore.setBestBlock(nextBlock.getIndex());
            this.metaStore.setBestBlockHash(nextBlock.getHash());

        }

        this.blockStore.put(nextBlock.getHash(), nextBlock);
        this.blockIndex.put(nextBlock.getIndex(), nextBlock.getHash());

        this.prevBlock = nextBlock;
        batchTxs(nextBlock);
        if (!listenerList.isEmpty() && broadcast) {
            listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
        }
        log.debug("Added idx=[{}], tx={}, branch={}, blockHash={}", nextBlock.getIndex(),
                nextBlock.getBodySize(), getBranchId(), nextBlock.getHash());
        return nextBlock;
    }

    private boolean isValidNewBlock(BlockHusk prevBlock, BlockHusk nextBlock) {
        if (prevBlock == null) {
            return true;
        }
        log.trace(" prev : " + prevBlock.getHash());
        log.trace(" new : " + nextBlock.getHash());

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.getHash().equals(nextBlock.getPrevHash())) {
            log.warn("invalid previous hash={}", prevBlock.getHash());
            return false;
        }

        return true;
    }

    public TransactionHusk addTransaction(TransactionHusk tx) {
        if (transactionStore.contains(tx.getHash())) {
            throw new FailedOperationException("Duplicated " + tx.getHash().toString()
                    + " Transaction");
        } else if (!tx.verify()) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            if (!listenerList.isEmpty()) {
                listenerList.forEach(listener -> listener.receivedTransaction(tx));
            }
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException("Transaction");
        }
    }

    public long size() {
        return blockIndex.size();
    }

    /**
     * Is valid chain boolean.
     *
     * @return the boolean
     */
    boolean isValidChain() {
        return isValidChain(this);
    }

    /**
     * Is valid chain boolean.
     *
     * @param blockChain the block chain
     * @return the boolean
     */
    private boolean isValidChain(BlockChain blockChain) {
        if (blockChain.getPrevBlock() != null) {
            BlockHusk block = blockChain.getPrevBlock(); // Get Last Block
            while (block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public BlockHusk getBlockByIndex(long idx) {
        Sha3Hash index = blockIndex.get(idx);
        if (index == null) {
            return null;
        }
        return blockStore.get(index);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(String hash) {
        return getBlockByHash(new Sha3Hash(hash));
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(Sha3Hash hash) {
        return blockStore.get(hash);
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     */
    TransactionHusk getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    /**
     * Is genesis block chain boolean.
     *
     * @return the boolean
     */
    private boolean isGenesisBlockChain() {
        return (this.prevBlock == null);
    }

    @SuppressWarnings("unchecked")
    private boolean executeTransaction(TransactionHusk tx) {
        try {
            return runtime.invoke(tx);
        } catch (Exception e) {
            log.error("executeTransaction Error" + e);
            return false;
        }
    }

    private Map<Sha3Hash, Boolean> executeBlock(BlockHusk block) {
        return runtime.invokeBlock(block);
    }

    private void batchTxs(BlockHusk block) {
        if (block == null || block.getBody() == null) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (TransactionHusk tx : block.getBody()) {
            keys.add(tx.getHash());
        }
        transactionStore.batch(keys);
    }

    @Override
    public String toString() {
        return "BlockChain{"
                + "genesisBlock=" + genesisBlock
                + ", prevBlock=" + prevBlock
                + ", height=" + this.getLastIndex()
                + '}';
    }

    public void close() {
        this.blockStore.close();
        this.transactionStore.close();
        this.metaStore.close();
        // TODO refactoring
        this.stateStore.close();
        this.transactionReceiptStore.close();

    }

    public String toStringStatus() {
        StringBuilder builder = new StringBuilder();

        builder.append("[BlockChain Status]\n")
                .append("genesisBlock=")
                .append(genesisBlock.getHash()).append("\n").append("currentBlock=" + "[")
                .append(prevBlock.getIndex()).append("]").append(prevBlock.getHash()).append("\n");

        String prevBlockHash = this.prevBlock.getPrevHash().toString();
        if (prevBlockHash == null) {
            prevBlockHash = "";
        }

        do {
            builder.append("<-- " + "[")
                    .append(blockStore.get(new Sha3Hash(prevBlockHash)).getIndex())
                    .append("]").append(prevBlockHash).append("\n");

            prevBlockHash = blockStore.get(new Sha3Hash(prevBlockHash)).getPrevHash().toString();

        } while (prevBlockHash != null
                && !prevBlockHash.equals(
                    "0000000000000000000000000000000000000000000000000000000000000000"));

        return builder.toString();

    }
}
