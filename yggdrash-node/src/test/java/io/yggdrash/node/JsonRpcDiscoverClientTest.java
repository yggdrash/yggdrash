package io.yggdrash.node;

import io.yggdrash.core.net.Peer;
import org.junit.Before;
import org.junit.Test;

public class JsonRpcDiscoverClientTest {

    private JsonRpcDiscoverClient client;

    @Before
    public void setUp() {
        this.client = new JsonRpcDiscoverClient();
    }

    @Test
    public void getPeersTest() {
        String seedHost = "localhost";
        int seedPort = 8080;
        String ynodeUri = "ynode://75bff16c@127.0.0.1:32919";
        Peer peer = Peer.valueOf(ynodeUri);
        try {
            assert !client.findPeers(seedHost, seedPort, peer).contains(ynodeUri);
            assert client.findPeers(seedHost, seedPort, peer).contains(ynodeUri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}