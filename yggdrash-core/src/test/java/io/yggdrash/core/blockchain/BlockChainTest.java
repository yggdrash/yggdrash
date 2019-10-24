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
import io.yggdrash.TestConstants.CiTest;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.PbftProto;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock<PbftProto.PbftBlock> testBlock = BlockChainTestUtils.createNextBlock(
                new ArrayList<>(), blockChainManager.getLastConfirmedBlock(), blockChain.getContractManager());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChainManager.getBlockByHash(testBlock.getHash())).isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
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
        ConsensusBlock block = BlockChainTestUtils.createNextBlockByPrevHash(
                prevHash, blockChain.getBlockChainManager().getLastConfirmedBlock());
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
        long index = 2L;
        ConsensusBlock block = BlockChainTestUtils.createSpecificHeightBlock(
                index,
                new ArrayList<>(),
                blockChain.getBlockChainManager().getLastConfirmedBlock(),
                blockChain.getContractManager());

        blockChain.addBlock(block, false);
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = generateTestBlockChain(true);

        ConsensusBlock<PbftProto.PbftBlock> testBlock = BlockChainTestUtils.createNextBlock(
                new ArrayList<>(),
                blockChain1.getBlockChainManager().getLastConfirmedBlock(),
                blockChain1.getContractManager());
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

        blockChain.close();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = generateTestBlockChain(true);

        ConsensusBlock<PbftProto.PbftBlock> testBlock = BlockChainTestUtils.createNextBlock(
                new ArrayList<>(),
                newDbBlockChain.getBlockChainManager().getLastConfirmedBlock(),
                newDbBlockChain.getContractManager());
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
        ConsensusBlock<PbftProto.PbftBlock> testBlock = BlockChainTestUtils.createNextBlock(
                new ArrayList<>(),
                blockChainManager.getLastConfirmedBlock(),
                blockChain.getContractManager());
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(BlockChainTestUtils.createTransferTx());
    }

    private static BlockChain generateTestBlockChain(boolean isProductionMode) {
        return BlockChainTestUtils.createBlockChain(isProductionMode);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ContractManager contractManager = blockChain.getContractManager();
        List<Transaction> blockBody = new ArrayList<>();
        ConsensusBlock block1 = BlockChainTestUtils.createNextBlock(
                blockBody,
                blockChainManager.getLastConfirmedBlock(),
                contractManager);
        blockChain.addBlock(block1, false);
        ConsensusBlock block2 = BlockChainTestUtils.createNextBlock(
                blockBody,
                blockChainManager.getLastConfirmedBlock(),
                contractManager);
        blockChain.addBlock(block2, false);
        return blockChain;
    }

}
