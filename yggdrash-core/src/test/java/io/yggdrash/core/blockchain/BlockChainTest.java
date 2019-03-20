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
import io.yggdrash.core.exception.NotValidateException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest extends CiTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTest.class);

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        BlockHusk testBlock = getBlockFixture(nextIndex, prevBlock.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByHash(testBlock.getHash()))
                .isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        BlockHusk testBlock = getBlockFixture(nextIndex, prevBlock.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByIndex(nextIndex)).isEqualTo(testBlock);
    }

    @Test
    public void shouldBeVerifiedBlockChain() {
        BlockChain blockChain = generateTestBlockChain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test(expected = NotValidateException.class)
    public void shouldBeExceptedNotValidateException() {
        BlockChain blockChain = generateTestBlockChain(false);
        Sha3Hash prevHash = new Sha3Hash("9358");
        BlockHusk block1 = getBlockFixture(1L, prevHash);
        blockChain.addBlock(block1, false);
        BlockHusk block2 =  getBlockFixture(2L, prevHash);
        blockChain.addBlock(block2, false);
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = generateTestBlockChain(true);
        BlockHusk genesisBlock = blockChain1.getGenesisBlock();

        BlockHusk testBlock = getBlockFixture(1L, genesisBlock.getHash());
        blockChain1.addBlock(testBlock, false);
        blockChain1.close();

        BlockChain blockChain2 = generateTestBlockChain(true);
        BlockHusk foundBlock = blockChain2.getBlockByHash(testBlock.getHash());
        blockChain2.close();
        long nextIndex = blockChain2.getLastIndex() + 1;
        assertThat(nextIndex).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);

        clearDefaultConfigDb();
    }

    @Test
    public void shouldBeStoredGenesisTxs() {
        BlockChain blockChain = generateTestBlockChain(true);
        BlockHusk genesis = blockChain.getGenesisBlock();
        for (TransactionHusk tx : genesis.getBody()) {
            assertThat(blockChain.getTxByHash(tx.getHash())).isNotNull();
        }
        assertThat(blockChain.countOfTxs()).isEqualTo(genesis.getBody().size());
        blockChain.close();
        clearDefaultConfigDb();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = generateTestBlockChain(true);
        BlockHusk genesisBlock = newDbBlockChain.getGenesisBlock();

        BlockHusk testBlock = getBlockFixture(1L, genesisBlock.getHash());
        newDbBlockChain.addBlock(testBlock, false);
        newDbBlockChain.generateBlock(TestConstants.wallet());
        assertThat(newDbBlockChain.getLastIndex()).isEqualTo(2);
        newDbBlockChain.close();

        BlockChain loadedDbBlockChain = generateTestBlockChain(true);
        loadedDbBlockChain.generateBlock(TestConstants.wallet());
        assertThat(loadedDbBlockChain.getLastIndex()).isEqualTo(3);
        clearDefaultConfigDb();
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = generateTestBlockChain(false);
        blockChain.addListener(new BranchEventListener() {
            @Override
            public void chainedBlock(BlockHusk block) {
                assertThat(block).isNotNull();
            }

            @Override
            public void receivedTransaction(TransactionHusk tx) {
                assertThat(tx).isNotNull();
            }
        });
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long nextIndex = blockChain.getLastIndex() + 1;
        BlockHusk testBlock = getBlockFixture(nextIndex, prevBlock.getHash());
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(BlockChainTestUtils.createTransferTxHusk());
    }

    private static BlockChain generateTestBlockChain(boolean isProductionMode) {
        return BlockChainTestUtils.createBlockChain(isProductionMode);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockHusk genesisBlock = blockChain.getGenesisBlock();
        BlockHusk block1 = getBlockFixture(1L, genesisBlock.getHash());
        blockChain.addBlock(block1, false);
        BlockHusk block2 = getBlockFixture(2L, block1.getHash());
        blockChain.addBlock(block2, false);
        return blockChain;
    }

    private static void clearDefaultConfigDb() {
        StoreTestUtils.clearDefaultConfigDb();
    }

    private static BlockHusk getBlockFixture(Long index, Sha3Hash prevHash) {

        try {
            Block tmpBlock = new Block(BlockChainTestUtils.genesisBlock().toJsonObject());
            BlockHeader tmpBlockHeader = tmpBlock.getHeader();
            BlockBody tmpBlockBody = tmpBlock.getBody();

            BlockHeader newBlockHeader = new BlockHeader(
                    tmpBlockHeader.getChain(),
                    tmpBlockHeader.getVersion(),
                    tmpBlockHeader.getType(),
                    prevHash.getBytes(),
                    index,
                    TimeUtils.time(),
                    tmpBlockBody);

            Block block = new Block(newBlockHeader, TestConstants.wallet(), tmpBlockBody);
            return new BlockHusk(block.toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

}
