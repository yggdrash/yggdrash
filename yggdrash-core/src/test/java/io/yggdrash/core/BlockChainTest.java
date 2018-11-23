package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = TestUtils.createBlockChain(false);
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long blockIndex = blockChain.size();
        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(blockIndex, prevBlock.getHash()));
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByHash(testBlock.getHash()))
                .isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long blockIndex = blockChain.size();
        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(blockIndex, prevBlock.getHash()));
        blockChain.addBlock(testBlock, false);

        assertThat(blockChain.getBlockByIndex(blockIndex))
                .isEqualTo(testBlock);
    }

    @Test
    public void shouldBeVerifiedBlockChain() {
        BlockChain blockChain = generateTestBlockChain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test(expected = NotValidateException.class)
    public void shouldBeExceptedNotValidateException() {
        BlockChain blockChain = TestUtils.createBlockChain(false);
        BlockHusk block1 = new BlockHusk(TestUtils.getBlockFixture(1L));
        blockChain.addBlock(block1, false);
        BlockHusk block2 = new BlockHusk(TestUtils.getBlockFixture(2L));
        blockChain.addBlock(block2, false);
        blockChain.isValidChain();
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = TestUtils.createBlockChain(true);
        BlockHusk genesisBlock = blockChain1.getGenesisBlock();

        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(1L, genesisBlock.getHash()));
        blockChain1.addBlock(testBlock, false);
        blockChain1.close();

        BlockChain blockChain2 = TestUtils.createBlockChain(true);
        BlockHusk foundBlock = blockChain2.getBlockByHash(testBlock.getHash());
        blockChain2.close();
        assertThat(blockChain2.size()).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);

        TestUtils.clearTestDb();
    }

    @Test
    public void shouldBeStoredGenesisTxs() {
        BlockChain blockChain = TestUtils.createBlockChain(true);
        BlockHusk genesis = blockChain.getGenesisBlock();
        for (TransactionHusk tx : genesis.getBody()) {
            assertThat(blockChain.getTxByHash(tx.getHash())).isNotNull();
        }
        assertThat(blockChain.countOfTxs()).isEqualTo(genesis.getBody().size());
        blockChain.close();
        TestUtils.clearTestDb();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = TestUtils.createBlockChain(true);
        BlockHusk genesisBlock = newDbBlockChain.getGenesisBlock();

        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(1L, genesisBlock.getHash()));
        newDbBlockChain.addBlock(testBlock, false);
        newDbBlockChain.generateBlock(TestUtils.wallet());
        assertThat(newDbBlockChain.getLastIndex()).isEqualTo(2);
        newDbBlockChain.close();

        BlockChain loadedDbBlockChain = TestUtils.createBlockChain(true);
        loadedDbBlockChain.generateBlock(TestUtils.wallet());
        assertThat(loadedDbBlockChain.getLastIndex()).isEqualTo(3);
        TestUtils.clearTestDb();
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = TestUtils.createBlockChain(false);
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
        long blockIndex = blockChain.size();
        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(blockIndex, prevBlock.getHash()));
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(TestUtils.createTransferTxHusk());
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = TestUtils.createBlockChain(false);
        BlockHusk genesisBlock = blockChain.getGenesisBlock();
        BlockHusk block1 = new BlockHusk(
                TestUtils.getBlockFixture(1L, genesisBlock.getHash()));
        blockChain.addBlock(block1, false);
        BlockHusk block2 = new BlockHusk(
                TestUtils.getBlockFixture(2L, block1.getHash()));
        blockChain.addBlock(block2, false);
        return blockChain;
    }

}
