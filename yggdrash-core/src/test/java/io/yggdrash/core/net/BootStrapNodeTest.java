package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.akashic.SimpleSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BootStrapNodeTest extends BootStrapNode {

    public BootStrapNodeTest() {
        BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
        setBranchGroup(branchGroup);
        setNodeStatus(NodeStatusMock.mock);
        setPeerNetwork(PeerNetworkMock.mock);
        setSyncManager(new SimpleSyncManager());
    }

    @Test
    public void bootstrappingTest() {
        bootstrapping();
    }

    @Test
    public void catchUpRequestTest() {
        catchUpRequest(BlockChainTestUtils.createNextBlock());
        assertEquals(1, branchGroup.getLastIndex(TestConstants.yggdrash()));
    }
}