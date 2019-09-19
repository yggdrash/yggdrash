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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BlockChainTest extends CiTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() {
        StoreTestUtils.clearDefaultConfigDb();
    }

    private Sha3Hash newBlockStateRoot;

    @Test
    public void stateRootOfBlockMustBeSameAsExecutedStateRoot() {
        /*
        TransactionStore 는 현재 pendingPool 의 pendingStateRoot 를 가지고 있으며,
        PbftService 에서 unconfirmedTxs 와 pendingStateRoot 를 가져와 newBlock 을 만든다.

        BlockChain 에서 addTransaction 이 실행될 때는 tmpStateStore 로 임시 실행 후,
        pendingStateStore 로 다시 한번 실행하여 executeStatus 가 SUCCESS 일때만 pendingPool
        에 담기고 그때 pendingStateRootHash 가 계산 및 세팅된다.

        BlockChain 에서 addBlock 이 실행될 때는 tmpStateStore 로 블록의 트랜잭션들을 실행하며
        이때 계산된 stateRootHash 는 block 의 stateRootHash 값과 같아야 한다.

        블록 실행 후 endBlock 이 실행되며 변경된 값이 존재하여 changedValues 가 존재하는 경우,
        pendingStateRoot 도 업에트한다.

        endBlock 실행 후 unconfirmedTxs 가 존재하는 경우 업데이트 된 stateRoot 와 stateStore 로
        세팅된 pendingStateStore 로 실행한 뒤 변경된 pendingStateRoot 도 업데이트한다.
        */

        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);

        Map<Sha3Hash, List<Transaction>> result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash genesisStateRoot = result.keySet().iterator().next();
        List<Transaction> unconfirmedTxs = result.get(genesisStateRoot);

        assertEquals(0, unconfirmedTxs.size());

        blockChain.addTransaction(createTx("1"));
        result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash pendingStateRoot1 = result.keySet().iterator().next();
        unconfirmedTxs = result.get(pendingStateRoot1);

        assertEquals(1, unconfirmedTxs.size());

        Block newBlock = makeNewBlock(blockChain, blockChain.getBlockChainManager().getLastIndex() + 1,
                blockChain.getBlockChainManager().getLastHash().getBytes());

        assertEquals(1, newBlock.getBody().getTransactionList().size());
        assertEquals(newBlockStateRoot, pendingStateRoot1);

        blockChain.addTransaction(createTx("2"));
        result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash pendingStateRoot12 = result.keySet().iterator().next();
        unconfirmedTxs = result.get(pendingStateRoot12);

        assertEquals(2, unconfirmedTxs.size());

        blockChain.addBlock(new PbftBlockMock(newBlock));
        result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash pendingStateRoot2 = result.keySet().iterator().next(); // executeBlock -> endBlock -> executePendingTxs
        unconfirmedTxs = result.get(pendingStateRoot2);

        assertNotEquals(pendingStateRoot12, pendingStateRoot2);
        assertEquals(1, unconfirmedTxs.size());

        Block newBlock2 = makeNewBlock(blockChain, blockChain.getBlockChainManager().getLastIndex() + 1,
                blockChain.getBlockChainManager().getLastHash().getBytes());

        assertEquals(1, newBlock2.getBody().getTransactionList().size());
        assertEquals(newBlockStateRoot, pendingStateRoot2);

        blockChain.addTransaction(createTx("3"));
        result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash pendingStateRoot23 = result.keySet().iterator().next();
        unconfirmedTxs = result.get(pendingStateRoot23);

        assertEquals(2, unconfirmedTxs.size());

        blockChain.addBlock(new PbftBlockMock(newBlock2));
        result = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        Sha3Hash pendingStateRoot3 = result.keySet().iterator().next(); // executeBlock -> endBlock -> executePendingTxs
        unconfirmedTxs = result.get(pendingStateRoot3);

        assertNotEquals(pendingStateRoot23, pendingStateRoot3);
        assertEquals(1, unconfirmedTxs.size());

        Block newBlock3 = makeNewBlock(blockChain, blockChain.getBlockChainManager().getLastIndex() + 1,
                blockChain.getBlockChainManager().getLastHash().getBytes());

        assertEquals(1, newBlock3.getBody().getTransactionList().size());
        assertEquals(newBlockStateRoot, pendingStateRoot3);
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
        // ret -> {pendingStateRootHash : unconfirmedTxs}
        Map<Sha3Hash, List<Transaction>> ret = blockChain.getBlockChainManager().getUnconfirmedTxsWithStateRoot();
        newBlockStateRoot = ret.keySet().iterator().next();
        List<Transaction> txList = ret.get(newBlockStateRoot);

        BlockBody newBlockBody = new BlockBody(txList);

        BlockHeader newBlockHeader = new BlockHeader(
                blockChain.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockStateRoot.getBytes(),
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
                    tmpBlockHeader.getStateRoot(),
                    tmpBlockBody);

            Block block = new BlockImpl(newBlockHeader, TestConstants.wallet(), tmpBlockBody);
            return new PbftBlockMock(block);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

}
