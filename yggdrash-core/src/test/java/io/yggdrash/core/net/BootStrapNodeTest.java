package io.yggdrash.core.net;

import org.junit.Assert;
import org.junit.Test;

public class BootStrapNodeTest extends BootStrapNode {

    public BootStrapNodeTest() {
        setPeerNetwork(PeerNetworkMock.mock);
        setSyncManager(BlockChainSyncManagerMock.mock);
    }

    @Test
    public void bootstrappingTest() {
        bootstrapping();
        Assert.assertTrue(BlockChainSyncManagerMock.nodeStatus.isUpStatus());
    }
}