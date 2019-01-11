package io.yggdrash.core.net;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeerTableTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private PeerTable peerTable;

    @Before
    public void setUp() {
        StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());
        PeerStore peerStore = storeBuilder.buildPeerStore();
        peerTable = new PeerTable(peerStore, OWNER);
    }

    @Test
    public void isPeerStoreEmptyTest() {
        assert peerTable.isPeerStoreEmpty();
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer);
        assert !peerTable.isPeerStoreEmpty();
    }

    @Test
    public void getLatestPeers() {
        assertEquals(peerTable.getBucketsCount(), 1);

        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer);

        Utils.sleep(2000);

        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");
        peerTable.addPeer(peer2);

        long touchedTime = peer2.getModified();
        List<Peer> latestPeerList = peerTable.getLatestPeers(touchedTime);

        assertEquals(latestPeerList.size(), 1);
        assertTrue(!latestPeerList.contains(peer));
        assertTrue(latestPeerList.contains(peer2));
    }
}