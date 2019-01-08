package io.yggdrash.core.net;

import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

public class DiscoverTaskTest {
    private static final int MAX_PEERS = 25;
    private static final BranchId BRANCH = TestConstants.STEM;
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());

    private DiscoverTask task;

    @Before
    public void setUp() {
        PeerGroup peerGroup = new PeerGroup(OWNER, MAX_PEERS);
        peerGroup.addPeerTable(BRANCH, storeBuilder.buildPeerStore(BRANCH));
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        this.task = new DiscoverTask(peerGroup, BRANCH) {
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