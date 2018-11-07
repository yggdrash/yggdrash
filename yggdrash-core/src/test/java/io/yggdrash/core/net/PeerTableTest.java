package io.yggdrash.core.net;

import io.yggdrash.core.BranchId;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

public class PeerTableTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final BranchId BRANCH = BranchId.stem();
    private PeerTable peerTable;

    @Before
    public void setUp() throws Exception {
        StoreBuilder storeBuilder = new StoreBuilder(false);
        PeerStore peerStore = storeBuilder.buildPeerStore(BRANCH);
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