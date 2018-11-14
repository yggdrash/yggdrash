package io.yggdrash.core.net;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

public class PeerTableTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private PeerTable peerTable;

    @Before
    public void setUp() {
        StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());
        PeerStore peerStore = storeBuilder.buildPeerStore(BranchId.NULL);
        peerTable = new PeerTable(peerStore, OWNER);
    }

    @Test
    public void isPeerStoreEmptyTest() {
        assert peerTable.isPeerStoreEmpty();
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer);
        assert !peerTable.isPeerStoreEmpty();
    }
}