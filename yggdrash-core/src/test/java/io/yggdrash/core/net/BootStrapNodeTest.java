package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BranchGroup;
import org.junit.Assert;
import org.junit.Test;

public class BootStrapNodeTest extends BootStrapNode {

    public BootStrapNodeTest() {
        BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
        setBranchGroup(branchGroup);
        setNodeStatus(NodeStatusMock.mock);
        setPeerNetwork(PeerNetworkMock.mock);
        setSyncManager(BlockChainSyncManagerMock.getMockWithBranchGroup(branchGroup));
    }

    @Test
    public void bootstrappingTest() {
        bootstrapping();
        Assert.assertTrue(nodeStatus.isUpStatus());
    }
}