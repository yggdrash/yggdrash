package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.akashic.SimpleSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import org.junit.Test;

public class BootStrapNodeTest {

    @Test
    public void bootstrappingTest() {
        YggdrashTestNode node1 = new YggdrashTestNode();
        node1.bootstrapping();
    }

    private class YggdrashTestNode extends BootStrapNode {
        YggdrashTestNode() {
            BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
            setBranchGroup(branchGroup);
            setNodeStatus(NodeStatusMock.mock);
            setPeerNetwork(PeerNetworkMock.mock);
            setSyncManager(new SimpleSyncManager());
        }
    }
}