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
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest extends CiTest {

    @After
    public void tearDown() {
        StoreTestUtils.clearDefaultConfigDb();
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain(false);
        Block block = blockChain.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        Block testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByHash(testBlock.getHash()))
                .isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        Block block = blockChain.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        Block testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByIndex(nextIndex)).isEqualTo(testBlock);
    }

    @Test(expected = NotValidateException.class)
    public void shouldBeExceptedNotValidateException() {
        BlockChain blockChain = generateTestBlockChain(false);
        Sha3Hash prevHash = new Sha3Hash("9358");
        Block block1 = getBlockFixture(1L, prevHash);
        blockChain.addBlock(block1, false);
        Block block2 =  getBlockFixture(2L, prevHash);
        blockChain.addBlock(block2, false);
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = generateTestBlockChain(true);
        Block genesisBlock = blockChain1.getGenesisBlock();

        Block testBlock = getBlockFixture(1L, genesisBlock.getHash());
        blockChain1.addBlock(testBlock, false);
        blockChain1.close();

        BlockChain blockChain2 = generateTestBlockChain(true);
        Block foundBlock = blockChain2.getBlockByHash(testBlock.getHash());
        blockChain2.close();
        long nextIndex = blockChain2.getLastIndex() + 1;
        assertThat(nextIndex).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);

    }

    @Test
    public void shouldBeStoredGenesisTxs() {
        BlockChain blockChain = generateTestBlockChain(true);
        Block genesis = blockChain.getGenesisBlock();
        List<TransactionHusk> txList = genesis.getBody();
        for (TransactionHusk tx : txList) {
            assertThat(blockChain.getTxByHash(tx.getHash())).isNotNull();
        }
        assertThat(blockChain.countOfTxs()).isEqualTo(genesis.getBody().size());
        blockChain.close();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = generateTestBlockChain(true);
        Block genesisBlock = newDbBlockChain.getGenesisBlock();

        Block testBlock = getBlockFixture(1L, genesisBlock.getHash());
        newDbBlockChain.addBlock(testBlock, false);
        assertThat(newDbBlockChain.getLastIndex()).isEqualTo(1);
        newDbBlockChain.close();

        BlockChain loadedDbBlockChain = generateTestBlockChain(true);
        assertThat(loadedDbBlockChain.getLastIndex()).isEqualTo(1);
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = generateTestBlockChain(false);
        blockChain.addListener(new BranchEventListener() {
            @Override
            public void chainedBlock(Block block) {
                assertThat(block).isNotNull();
            }

            @Override
            public void receivedTransaction(TransactionHusk tx) {
                assertThat(tx).isNotNull();
            }
        });
        Block block = blockChain.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        Block testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(BlockChainTestUtils.createTransferTxHusk());
    }

    private static BlockChain generateTestBlockChain(boolean isProductionMode) {
        return BlockChainTestUtils.createBlockChain(isProductionMode);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = generateTestBlockChain(false);
        Block genesisBlock = blockChain.getGenesisBlock();
        Block block1 = getBlockFixture(1L, genesisBlock.getHash());
        blockChain.addBlock(block1, false);
        Block block2 = getBlockFixture(2L, block1.getHash());
        blockChain.addBlock(block2, false);
        return blockChain;
    }

    private static Block getBlockFixture(Long index, Sha3Hash prevHash) {
        return getBlockFixture(index, prevHash.getBytes());
    }

    private static Block getBlockFixture(Long index, byte[] prevHash) {

        try {
            io.yggdrash.core.blockchain.Block tmpBlock = new io.yggdrash.core.blockchain.Block(BlockChainTestUtils.genesisBlock().toJsonObject());
            BlockHeader tmpBlockHeader = tmpBlock.getHeader();
            BlockBody tmpBlockBody = tmpBlock.getBody();

            BlockHeader newBlockHeader = new BlockHeader(
                    tmpBlockHeader.getChain(),
                    tmpBlockHeader.getVersion(),
                    tmpBlockHeader.getType(),
                    prevHash,
                    index,
                    TimeUtils.time(),
                    tmpBlockBody);

            io.yggdrash.core.blockchain.Block block = new io.yggdrash.core.blockchain.Block(newBlockHeader, TestConstants.wallet(), tmpBlockBody);
            return new BlockHusk(block);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

}
