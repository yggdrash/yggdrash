package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BlockChainServiceConsumerTest {
    private BranchGroup branchGroup;
    private BlockChainServiceConsumer blockChainServiceConsumer;
    private static final BranchId branchId = TestConstants.yggdrash();
    private BlockChain branch;

    @Before
    public void setUp() {
        this.branchGroup = BlockChainTestUtils.createBranchGroup();
        this.branch = branchGroup.getBranch(branchId);
        blockChainServiceConsumer = new BlockChainServiceConsumer(branchGroup);
    }

    @Test
    public void syncBlock() {
        BlockChainTestUtils.generateBlock(branchGroup, branchId);
        Assert.assertEquals(1, branch.getLastIndex());

        blockChainServiceConsumer.setListener(BlockChainSyncManagerMock.getMockWithBranchGroup(branchGroup));
        List<ConsensusBlock> blockList = blockChainServiceConsumer.syncBlock(branchId, 1, 10);

        Assert.assertEquals(1, blockList.size());
        Assert.assertEquals(1, branch.getLastIndex());
    }

    @Test
    public void syncBlockByPassingTheLimitSize() {
        TestConstants.SlowTest.apply();
        // arrange
        int height = 110;
        List<ConsensusBlock> blockList = BlockChainTestUtils.createBlockListFilledWithTx(height, 100);

        blockList.forEach(b -> branch.addBlock(b, false));
        Assert.assertEquals(height, branch.getLastIndex());

        // act
        List<ConsensusBlock> received = blockChainServiceConsumer.syncBlock(branchId, 1, height);

        // assert
        Assert.assertEquals(106, received.size());
    }

    @Test
    public void syncBLockRequestingCatchUp() {
        BlockChainTestUtils.setBlockHeightOfBlockChain(branch, 10);

        List<ConsensusBlock> blockList = blockChainServiceConsumer.syncBlock(branchId, 3, 10);

        Assert.assertEquals(8, blockList.size());
        Assert.assertEquals(10, branch.getLastIndex());
    }

    @Test
    public void syncTx() {
        assertEquals(0, blockChainServiceConsumer.syncTx(branchId).size());

        blockChainServiceConsumer.broadcastTx(BlockChainTestUtils.createTransferTxHusk());

        assertEquals(1, blockChainServiceConsumer.syncTx(branchId).size());
    }

    @Test
    public void broadcastBlock() {
        assertEquals(0, branchGroup.getBranch(branchId).getLastIndex());

        blockChainServiceConsumer.broadcastBlock(BlockChainTestUtils.createNextBlock());

        assertEquals(1, branchGroup.getBranch(branchId).getLastIndex());
    }
}