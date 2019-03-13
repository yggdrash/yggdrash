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
        branchGroup.generateBlock(TestConstants.wallet(), branchId);
        blockChainServiceConsumer.setListener(BlockChainSyncManagerMock.getMockWithBranchGroup(branchGroup));
        Assert.assertEquals(1, branch.getLastIndex());

        List<BlockHusk> blockHuskList =
                blockChainServiceConsumer.syncBlock(branchId, 1, 10);

        Assert.assertEquals(1, blockHuskList.size());
        Assert.assertEquals(1, branch.getLastIndex());
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