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

package io.yggdrash.validator.data;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainManagerImpl;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.store.BlockChainStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockChainManagerMock<T> implements BlockChainManager<T> {

    private final BlockChainManager<T> blockChainManager;

    public BlockChainManagerMock(BlockChainStore store) {
        this.blockChainManager = new BlockChainManagerImpl<>(store);
    }

    @Override
    public void initGenesis(Block genesisBlock) {
        blockChainManager.initGenesis(genesisBlock);
    }

    @Override
    public void loadTransaction() {
        blockChainManager.loadTransaction();
    }

    @Override
    public int verify(ConsensusBlock<T> block) {
        return blockChainManager.verify(block);
    }

    @Override
    public int verify(Transaction transaction) {
        return blockChainManager.verify(transaction);
    }

    @Override
    public void addBlock(ConsensusBlock<T> nextBlock) {
        blockChainManager.addBlock(nextBlock);
    }

    @Override
    public void batchTxs(ConsensusBlock<T> block, Sha3Hash stateRoot) {
        blockChainManager.batchTxs(block, stateRoot);
    }

    /*
    @Override
    public void addBlock(ConsensusBlock<T> nextBlock, Sha3Hash stateRoot) {
        blockChainManager.addBlock(nextBlock, stateRoot);
    }
    */

    @Override
    public void addTransaction(Transaction tx) {
        blockChainManager.addTransaction(tx);
    }

    @Override
    public void addTransaction(Transaction tx, Sha3Hash stateRootHash) {
        blockChainManager.addTransaction(tx, stateRootHash);
    }

    @Override
    public void flushUnconfirmedTxs(Set<Sha3Hash> keys) {
        blockChainManager.flushUnconfirmedTxs(keys);
    }

    @Override
    public void flushUnconfirmedTx(Sha3Hash key) {
        blockChainManager.flushUnconfirmedTx(key);
    }

    @Override
    public int getUnconfirmedTxsSize() {
        return blockChainManager.getUnconfirmedTxsSize();
    }

    /*
    @Override
    public void setPendingStateRoot(Sha3Hash stateRootHash) {
        blockChainManager.setPendingStateRoot(stateRootHash);
    }
    */

    @Override
    public void updateTxCache(Block block) {
        blockChainManager.updateTxCache(block);
    }

    @Override
    public ConsensusBlock<T> getLastConfirmedBlock() {
        return blockChainManager.getLastConfirmedBlock();
    }

    @Override
    public ConsensusBlock<T> getBlockByHash(Sha3Hash hash) {
        return blockChainManager.getBlockByHash(hash);
    }

    @Override
    public ConsensusBlock<T> getBlockByIndex(long index) {
        return blockChainManager.getBlockByIndex(index);
    }

    @Override
    public Transaction getTxByHash(Sha3Hash hash) {
        return blockChainManager.getTxByHash(hash);
    }

    @Override
    public Collection<Transaction> getRecentTxs() {
        return blockChainManager.getRecentTxs();
    }

    @Override
    public List<Transaction> getUnconfirmedTxsWithLimit(long limit) {
        return blockChainManager.getUnconfirmedTxsWithLimit(limit);
    }

    @Override
    public List<Transaction> getUnconfirmedTxs() {
        return blockChainManager.getUnconfirmedTxs();
    }

    @Override
    public Map<Sha3Hash, List<Transaction>> getUnconfirmedTxsWithStateRoot() {
        return blockChainManager.getUnconfirmedTxsWithStateRoot();
    }

    @Override
    public Receipt getReceipt(String txId) {
        return blockChainManager.getReceipt(txId);
    }

    @Override
    public long getLastIndex() {
        return blockChainManager.getLastIndex();
    }

    @Override
    public Sha3Hash getLastHash() {
        return blockChainManager.getLastHash();
    }

    @Override
    public long countOfTxs() {
        return blockChainManager.countOfTxs();
    }

    @Override
    public long countOfBlocks() {
        return blockChainManager.countOfBlocks();
    }

    @Override
    public boolean containsBlockHash(Sha3Hash blockHash) {
        return blockChainManager.containsBlockHash(blockHash);
    }

    @Override
    public boolean containsTxHash(Sha3Hash txHash) {
        return blockChainManager.containsTxHash(txHash);
    }

    @Override
    public boolean contains(Block block) {
        return blockChainManager.contains(block);
    }

    @Override
    public boolean contains(Transaction transaction) {
        return blockChainManager.contains(transaction);
    }

    @Override
    public void close() {
        blockChainManager.close();
    }
}