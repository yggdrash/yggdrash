package io.yggdrash.core.net;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

public class DiscoverTaskTest {
    private static final int MAX_PEERS = 25;
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());

    private DiscoverTask task;

    @Before
    public void setUp() {
        PeerGroup peerGroup = new PeerGroup(OWNER, storeBuilder.buildPeerStore(), MAX_PEERS);
        peerGroup.addPeerByYnodeUri("ynode://75bff16c@127.0.0.1:32918");
        this.task = new DiscoverTask(peerGroup) {
            @Override
            public PeerClientChannel getClient(Peer peer) {
                return ChannelMock.dummy();
            }
        };
    }

    @Test
    public void getClientTest() {
        assert task.getClient(OWNER).getPeer() != null;
    }

    @Test
    public void runTest() {
        task.run();
    }

}