package io.yggdrash.node;

import io.yggdrash.core.net.Peer;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        String ynodeUri
                = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc93"
                + "37142728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:8083";
        Peer peer = Peer.valueOf(ynodeUri);
        try {
            assertThat(client.findPeers(seedHost, seedPort, peer)).doesNotContain(ynodeUri);
            assertThat(client.findPeers(seedHost, seedPort, peer)).contains(ynodeUri);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}