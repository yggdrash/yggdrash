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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.TestConstants.CiTest;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlockChainTest extends CiTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() {
        StoreTestUtils.clearDefaultConfigDb();
    }

    // This test is related to AddTransaction & AddBlock
    @Test
    public void onlySuccessTxRemainInPendingPool() {
        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);
        // Transactions received by the API are executed as the tmpStateStore and returned to success status.
        // SuccessTx -> Transferred successfully
        Map<String, List<String>> errorLogs = blockChain.addTransaction(createTx("400000000000000000000"));
        assertEquals(0, errorLogs.size()); // No errorLogs returned
        errorLogs = blockChain.addTransaction(createTx("400000000000000000000"));
        assertEquals(0, errorLogs.size());
        // ErrTx -> Insufficient funds
        errorLogs = blockChain.addTransaction(createTx("400000000000000000000"));
        assertEquals(0, errorLogs.size());
        // Transactions in pendingPool execute on the current state, so the third error tx
        // cannot be added to the pendingPool. Only success txs can be added.
        assertEquals(2, blockChain.getBlockChainManager().getUnconfirmedTxs().size());

        // Create a new block with unconfirmedTxs in pbftService.
        Block newBlock = makeNewBlock(blockChain, blockChain.getBlockChainManager().getLastIndex() + 1,
                blockChain.getBlockChainManager().getLastHash().getBytes());
        // Only success txs can be added to the blockBody.
        assertEquals(2, newBlock.getBody().getTransactionList().size());

        // Transaction transfer through the API can also occur while the block is being created.
        // SuccessTx -> Transferred successfully
        errorLogs = blockChain.addTransaction(createTx("100000000000000000000"));
        assertEquals(0, errorLogs.size());
        // ErrTx -> Insufficient funds
        errorLogs = blockChain.addTransaction(createTx("100000000000000000000"));
        assertEquals(0, errorLogs.size());
        errorLogs = blockChain.addTransaction(createTx("100000000000000000000"));
        assertEquals(0, errorLogs.size());
        // 3 success txs are remaining in pendingPool.
        assertEquals(3, blockChain.getBlockChainManager().getUnconfirmedTxs().size());

        // The transactions within that block are removed from the pendingPool when the created block is added.
        errorLogs = blockChain.addBlock(new PbftBlockMock(newBlock));
        // No errorLogs returned when adding block is succeeded.
        assertEquals(0, errorLogs.size());
        // Once the block is added to the blockChain, run txs of pendingPool again with the updated stateStore
        // to remove any remaining error txs. Only success tx will remain in pendingPool.
        assertEquals(1, blockChain.getBlockChainManager().getUnconfirmedTxs().size());
    }

    // Create a tx that decimal applied
    private Transaction createTx(String amountStr) {
        BigInteger amount = new BigInteger(amountStr).multiply(BigInteger.TEN.pow(18));
        return BlockChainTestUtils.createTransferTx(TestConstants.TRANSFER_TO, amount);
    }

    // The same function of pbftService
    private Block makeNewBlock(BlockChain blockChain, long index, byte[] prevBlockHash) {
        List<Transaction> txList = new ArrayList<>(blockChain.getBlockChainManager().getUnconfirmedTxs());

        BlockBody newBlockBody = new BlockBody(txList);

        BlockHeader newBlockHeader = new BlockHeader(
                blockChain.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new BlockImpl(newBlockHeader, TestConstants.wallet(), newBlockBody);
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;
        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChainManager.getBlockByHash(testBlock.getHash())).isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;
        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        Map<String, List<String>> errorLogs = blockChain.addBlock(block, false);

        assertEquals(1, errorLogs.size());
        assertTrue(errorLogs.keySet().contains("BusinessError"));
        assertEquals(1, errorLogs.get("BusinessError").size());
        assertTrue(errorLogs.get("BusinessError").contains("Unknown BlockHeight"));
    }

    @Test
    public void invalidBlockHashExceptionMustOccur() {
        BlockChain blockChain = generateTestBlockChain(false);
        Sha3Hash prevHash = new Sha3Hash("9358");
        ConsensusBlock block = getBlockFixture(1L, prevHash);
        Map<String, List<String>> errorLogs = blockChain.addBlock(block, false);

        assertEquals(1, errorLogs.size());
        assertTrue(errorLogs.keySet().contains("BusinessError"));
        assertEquals(2, errorLogs.get("BusinessError").size());
        assertTrue(errorLogs.get("BusinessError").contains("Invalid data format"));
        assertTrue(errorLogs.get("BusinessError").contains("Invalid BlockHash"));
    }

    @Test
    public void unknownBlockHeightExceptionNustOccur() {
        BlockChain blockChain = generateTestBlockChain(false);
        Sha3Hash prevHash = new Sha3Hash("9358");
        ConsensusBlock block = getBlockFixture(2L, prevHash);
        blockChain.addBlock(block, false);
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = generateTestBlockChain(true);
        ConsensusBlock genesisBlock = blockChain1.getGenesisBlock();

        ConsensusBlock testBlock = getBlockFixture(1L, genesisBlock.getHash());
        blockChain1.addBlock(testBlock, false);
        blockChain1.close();

        BlockChain blockChain2 = generateTestBlockChain(true);
        BlockChainManager blockChain2Manager = blockChain2.getBlockChainManager();
        ConsensusBlock foundBlock = blockChain2Manager.getBlockByHash(testBlock.getHash());
        blockChain2.close();
        long nextIndex = blockChain2Manager.getLastIndex() + 1;

        assertThat(nextIndex).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);
    }

    @Test
    public void shouldBeStoredGenesisTxs() {
        BlockChain blockChain = generateTestBlockChain(true);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock genesis = blockChain.getGenesisBlock();
        List<Transaction> txList = genesis.getBody().getTransactionList();

        for (Transaction tx : txList) {
            assertThat(blockChainManager.getTxByHash(tx.getHash())).isNotNull();
        }

        assertThat(blockChainManager.countOfTxs()).isEqualTo(genesis.getBody().getCount());
        blockChain.close();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = generateTestBlockChain(true);
        ConsensusBlock genesisBlock = newDbBlockChain.getGenesisBlock();

        ConsensusBlock testBlock = getBlockFixture(1L, genesisBlock.getHash());
        newDbBlockChain.addBlock(testBlock, false);
        assertThat(newDbBlockChain.getBlockChainManager().getLastIndex()).isEqualTo(1);
        newDbBlockChain.close();

        BlockChain loadedDbBlockChain = generateTestBlockChain(true);
        assertThat(loadedDbBlockChain.getBlockChainManager().getLastIndex()).isEqualTo(1);
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        blockChain.addListener(new BranchEventListener() {
            @Override
            public void chainedBlock(ConsensusBlock block) {
                assertThat(block).isNotNull();
            }

            @Override
            public void receivedTransaction(Transaction tx) {
                assertThat(tx).isNotNull();
            }
        });
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;

        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(BlockChainTestUtils.createTransferTx());
    }

    private static BlockChain generateTestBlockChain(boolean isProductionMode) {
        return BlockChainTestUtils.createBlockChain(isProductionMode);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = generateTestBlockChain(false);
        ConsensusBlock genesisBlock = blockChain.getGenesisBlock();
        ConsensusBlock block1 = getBlockFixture(1L, genesisBlock.getHash());
        blockChain.addBlock(block1, false);
        ConsensusBlock block2 = getBlockFixture(2L, block1.getHash());
        blockChain.addBlock(block2, false);
        return blockChain;
    }

    private static ConsensusBlock getBlockFixture(Long index, Sha3Hash prevHash) {
        return getBlockFixture(index, prevHash.getBytes(), null);
    }

    private static ConsensusBlock getBlockFixture(Long index, Sha3Hash prevHash, String branchId) {
        return getBlockFixture(index, prevHash.getBytes(), BranchId.of(branchId).getBytes());
    }

    private static ConsensusBlock getBlockFixture(Long index, byte[] prevHash, byte[] branchId) {

        try {
            Block tmpBlock = BlockChainTestUtils.genesisBlock().getBlock();
            BlockHeader tmpBlockHeader = tmpBlock.getHeader();
            BlockBody tmpBlockBody = tmpBlock.getBody();
            byte[] chain = branchId != null ? branchId : tmpBlockHeader.getChain();

            BlockHeader newBlockHeader = new BlockHeader(
                    chain,
                    tmpBlockHeader.getVersion(),
                    tmpBlockHeader.getType(),
                    prevHash,
                    index,
                    TimeUtils.time(),
                    tmpBlockBody);

            Block block = new BlockImpl(newBlockHeader, TestConstants.wallet(), tmpBlockBody);
            return new PbftBlockMock(block);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

}
