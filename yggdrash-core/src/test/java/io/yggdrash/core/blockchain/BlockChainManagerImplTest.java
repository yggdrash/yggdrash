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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.proto.PbftProto;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockChainManagerImplTest {

    private BlockChainManagerImpl<PbftProto.PbftBlock> blockChainManager;

    @Before
    public void setUp() throws Exception {
        ConsensusBlockStore<PbftProto.PbftBlock> pbftBlockStore = new PbftBlockStoreMock(new HashMapDbSource());
        TransactionStore transactionStore = new TransactionStore(new HashMapDbSource());
        TransactionReceiptStore transactionReceiptStore = new TransactionReceiptStore(new HashMapDbSource());
        this.blockChainManager
                = new BlockChainManagerImpl<>(pbftBlockStore, transactionStore, transactionReceiptStore);
    }

    @Test
    public void addTransactionTest() {
        Transaction tx = BlockChainTestUtils.createTransferTx();
        Transaction invalidTx = BlockChainTestUtils.createInvalidTransferTx();

        assertEquals(32000, blockChainManager.addTransaction(tx)); // valid (success)
        assertEquals(32016, blockChainManager.addTransaction(tx)); // duplicated
        assertEquals(32035, blockChainManager.addTransaction(invalidTx)); //timeout, invalid format, untrusted
        assertEquals(0, blockChainManager.getRecentTxs().size()); // haven't done batchTx yet
        assertEquals(0, blockChainManager.countOfTxs());
        assertEquals(1, blockChainManager.getUnconfirmedTxs().size()); // txPool only contains valid tx
        assertTrue(blockChainManager.getUnconfirmedTxs().contains(tx)); // txPool contains tx
        assertTrue(blockChainManager.contains(tx));
        assertFalse(blockChainManager.contains(invalidTx));
        assertEquals(tx, blockChainManager.getTxByHash(tx.getHash()));
    }

    @Test
    public void addBlockTest() {
        // verify block
        ConsensusBlock<PbftProto.PbftBlock> genesisBlock = BlockChainTestUtils.genesisBlock();
        assertEquals(32000, blockChainManager.verify(genesisBlock));
        blockChainManager.addBlock(genesisBlock);
        assertEquals(0, blockChainManager.getLastIndex());
        assertEquals(blockChainManager.getLastHash(), genesisBlock.getBlock().getHash());
        assertEquals(blockChainManager.getLastConfirmedBlock(), genesisBlock);
        assertEquals(0, blockChainManager.getUnconfirmedTxs().size());
        assertEquals(1, blockChainManager.countOfBlocks());

        ConsensusBlock<PbftProto.PbftBlock> block = generateBlockWithTxs(true);
        assertEquals(32000, blockChainManager.verify(block));
        blockChainManager.addBlock(block);
        assertTrue(blockChainManager.contains(block));
        assertEquals(block, blockChainManager.getBlockByHash(block.getHash()));
        assertEquals(block, blockChainManager.getBlockByIndex(1));
        assertEquals(block, blockChainManager.getLastConfirmedBlock());
        assertEquals(block.getHash(),blockChainManager.getLastHash());
        assertEquals(2, blockChainManager.countOfBlocks());
        assertEquals(0, blockChainManager.getUnconfirmedTxs().size());
        assertEquals(10, blockChainManager.getRecentTxs().size());
        assertEquals(10, blockChainManager.countOfTxs());

        assertEquals(32084, blockChainManager.verify(block)); //blockHeight, blockHash, duplicated

        block.getBlock().getBody().getTransactionList().add(BlockChainTestUtils.createTransferTx());
        assertEquals(32124, blockChainManager.verify(block)); // + invalid format, invalid merkleRoot

        ConsensusBlock<PbftProto.PbftBlock> blockWithInvalidTx = generateBlockWithTxs(false);
        assertEquals(32000, blockChainManager.verify(blockWithInvalidTx));
        blockChainManager.addBlock(blockWithInvalidTx);
        assertEquals(3, blockChainManager.countOfBlocks());
        assertEquals(20, blockChainManager.countOfTxs());
        assertEquals(20, blockChainManager.getRecentTxs().size()); //invalid tx was excluded
    }

    private ConsensusBlock<PbftProto.PbftBlock> generateBlockWithTxs(Boolean valid) {
        for (int i = 0; i < 10; i++) {
            blockChainManager.addTransaction(BlockChainTestUtils.createTransferTx());
        }
        List<Transaction> txs =
                    blockChainManager.getUnconfirmedTxsWithLimit(Constants.Limit.BLOCK_SYNC_SIZE);
        if (!valid) {
            txs.add(BlockChainTestUtils.createInvalidTransferTx());
        }
        return BlockChainTestUtils.createNextBlock(txs, blockChainManager.getLastConfirmedBlock());
    }

}