package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.PbftProto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BlockServiceConsumerTest {
    private BranchGroup branchGroup;
    private BlockServiceConsumer<PbftProto.PbftBlock> blockServiceConsumer;
    private static final BranchId branchId = TestConstants.yggdrash();
    private BlockChain branch;
    private BlockChainManager blockChainManager;

    @Before
    public void setUp() {
        this.branchGroup = BlockChainTestUtils.createBranchGroup();
        this.branch = branchGroup.getBranch(branchId);
        this.blockChainManager = branch.getBlockChainManager();
        blockServiceConsumer = new BlockServiceConsumer<>(branchGroup);
    }

    @Test
    public void syncBlock() {
        BlockChainTestUtils.generateBlock(branchGroup, branchId);
        Assert.assertEquals(1, blockChainManager.getLastIndex());

        blockServiceConsumer.setListener(BlockChainSyncManagerMock.mock);
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = blockServiceConsumer.syncBlock(branchId, 1, 10);

        Assert.assertEquals(1, blockList.size());
        Assert.assertEquals(1, blockChainManager.getLastIndex());
    }

    @Test
    public void syncBlockByPassingTheLimitSize() {
        TestConstants.SlowTest.apply();
        // arrange
        int height = 110;
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList =
                BlockChainTestUtils.createBlockListWithTxs(height, 100, branch.getContractManager());

        blockList.forEach(b -> branch.addBlock(b, false));
        Assert.assertEquals(height, blockChainManager.getLastIndex());

        // act
        List<ConsensusBlock<PbftProto.PbftBlock>> received = blockServiceConsumer.syncBlock(branchId, 1, height);

        // assert
        Assert.assertTrue(height > received.size());
    }

    @Test
    public void syncBLockRequestingCatchUp() {
        BlockChainTestUtils.setBlockHeightOfBlockChain(branch, 10);

        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = blockServiceConsumer.syncBlock(branchId, 3, 10);

        Assert.assertEquals(8, blockList.size());
        Assert.assertEquals(10, blockChainManager.getLastIndex());
    }

    @Test
    public void broadcastBlock() {
        assertEquals(0, branchGroup.getBranch(branchId).getBlockChainManager().getLastIndex());

        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock();
        blockServiceConsumer.broadcastBlock(nextBlock);
        blockServiceConsumer.broadcastBlock(nextBlock);

        assertEquals(1, branchGroup.getBranch(branchId).getBlockChainManager().getLastIndex());
    }
}