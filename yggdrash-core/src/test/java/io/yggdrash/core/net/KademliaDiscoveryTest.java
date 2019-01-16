package io.yggdrash.core.net;

import org.junit.Before;
import org.junit.Test;

public class KademliaDiscoveryTest {
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");

    private KademliaDiscovery discovery;

    @Before
    public void setUp() {
        this.discovery = new KademliaDiscoveryMock(OWNER);
    }

    @Test
    public void getPeerHandlerTest() {
        assert discovery.getPeerHandler(OWNER).getPeer() != null;
    }

    @Test
    public void runTest() {
        discovery.discover();
    }
}