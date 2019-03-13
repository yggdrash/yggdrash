package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        blockChainServiceConsumer.setListener(BlockChainSyncManagerMock.getMockWithBranchGroup(branchGroup));

        Assert.assertEquals(0, branch.getLastIndex());

        List<BlockHusk> blockHuskList =
                blockChainServiceConsumer.syncBlock(branchId, 1, 10);

        Assert.assertEquals(0, blockHuskList.size());
        Assert.assertEquals(99, branch.getLastIndex());
    }

    @Test
    public void syncBLockRequestingCatchUp() {
        BlockChainTestUtils.setBlockHeightOfBlockChain(branch, 10);

        List<BlockHusk> blockHuskList =
                blockChainServiceConsumer.syncBlock(branchId, 3, 10);

        Assert.assertEquals(8, blockHuskList.size());
        Assert.assertEquals(10, branch.getLastIndex());
    }

    @Test
    public void syncTx() {
        Assert.assertEquals(blockChainServiceConsumer.syncTx(branchId).size(), 0);

        blockChainServiceConsumer.broadcastTx(BlockChainTestUtils.createTransferTxHusk());

        Assert.assertEquals(blockChainServiceConsumer.syncTx(branchId).size(), 1);
    }

    @Test
    public void broadcastBlock() {
        Assert.assertEquals(branchGroup.getBranch(branchId).getLastIndex(), 0);

        blockChainServiceConsumer.broadcastBlock(BlockChainTestUtils.createNextBlock());

        Assert.assertEquals(branchGroup.getBranch(branchId).getLastIndex(), 1);
    }
}