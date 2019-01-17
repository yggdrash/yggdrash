package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import org.junit.Test;

public class KademliaDiscoveryTest {

    @Test
    public void runTest() {
        Discovery discovery = new KademliaDiscovery(PeerTestUtils.createPeerTable());
        discovery.discover(PeerHandlerMock.factory);
    }
}