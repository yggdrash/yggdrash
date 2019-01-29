package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import org.junit.Test;

public class YggdrashBootStrapNodeTest {

    @Test
    public void bootstrapping() {
        YggdrashTestNode node1 = new YggdrashTestNode();
        node1.bootstrapping(node1.getDiscovery(), 1);
    }

    private class YggdrashTestNode extends BootStrapNode {
        YggdrashTestNode() {
            this.peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
        }

        Discovery getDiscovery() {
            return new KademliaDiscovery(PeerTestUtils.createPeerTable());
        }
    }
}