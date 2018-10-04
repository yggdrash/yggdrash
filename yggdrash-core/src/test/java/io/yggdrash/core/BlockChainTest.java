package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.core.event.BranchEventListener;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {
    private File sampleBranchInfo;

    @Before
    public void init() {
        sampleBranchInfo = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("branch-sample.json")).getFile());
    }

    @After
    public void tearDown() {
        TestUtils.clearTestDb();
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain();
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        long blockIndex = blockChain.size();
        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(blockIndex, prevBlock.getHash()));
        blockChain.addBlock(testBlock);

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
        blockChain.addBlock(testBlock);

        assertThat(blockChain.getBlockByIndex(blockIndex))
                .isEqualTo(testBlock);
    }

    @Test
    public void shouldBeVerifiedBlockChain() {
        BlockChain blockChain = generateTestBlockChain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
        BlockHusk genesisBlock = blockChain.getGenesisBlock();
        BlockHusk block1 = new BlockHusk(
                TestUtils.getBlockFixture(1L, genesisBlock.getHash()));
        blockChain.addBlock(block1);
        BlockHusk block2 = new BlockHusk(
                TestUtils.getBlockFixture(2L, block1.getHash()));
        blockChain.addBlock(block2);
        return blockChain;
    }

    @Test(expected = NotValidateException.class)
    public void shouldBeExceptedNotValidateException() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
        BlockHusk block1 = new BlockHusk(TestUtils.getBlockFixture(1L));
        blockChain.addBlock(block1);
        BlockHusk block2 = new BlockHusk(TestUtils.getBlockFixture(2L));
        blockChain.addBlock(block2);
        blockChain.isValidChain();
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
        BlockHusk genesisBlock = blockChain.getGenesisBlock();

        BlockHusk testBlock = new BlockHusk(
                TestUtils.getBlockFixture(1L, genesisBlock.getHash()));
        blockChain.addBlock(testBlock);
        blockChain.close();

        BlockChain otherBlockChain = new BlockChain(sampleBranchInfo);
        BlockHusk foundBlock = otherBlockChain.getBlockByHash(testBlock.getHash());
        assertThat(otherBlockChain.size()).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);
    }

    @Test
    public void shouldBeCreatedNewBlockChain() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
        assertThat(blockChain.size()).isEqualTo(1L);
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
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
        blockChain.addBlock(testBlock);
        blockChain.addTransaction(TestUtils.createTransferTxHusk());
    }

}
