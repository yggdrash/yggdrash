package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class BlockChainServiceConsumerTest {
    private BranchGroup branchGroup;
    private BlockChainServiceConsumer blockChainServiceConsumer;
    private static final BranchId branchId = TestConstants.yggdrash();

    @Before
    public void setUp() {
        this.branchGroup = BlockChainTestUtils.createBranchGroup();
        blockChainServiceConsumer = new BlockChainServiceConsumer(branchGroup);
    }

    @Test
    public void syncBlock() {
        List<BlockHusk> blockHuskList = blockChainServiceConsumer.syncBlock(branchId, 0, 10);

        assert blockHuskList.size() == 1;
    }

    @Test
    public void syncTransaction() {
        assert blockChainServiceConsumer.syncTransaction(branchId).size() == 0;

        blockChainServiceConsumer.broadcastTransaction(BlockChainTestUtils.createTransferTxHusk());

        assert blockChainServiceConsumer.syncTransaction(branchId).size() == 1;
    }

    @Test
    public void broadcastBlock() {
        assert branchGroup.getBranch(branchId).getLastIndex() == 0;

        blockChainServiceConsumer.broadcastBlock(BlockChainTestUtils.createNextBlock());

        assert branchGroup.getBranch(branchId).getLastIndex() == 1;
    }
}