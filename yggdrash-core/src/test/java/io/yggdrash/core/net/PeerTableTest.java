package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.util.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeerTableTest {
    private PeerTable peerTable;

    @Before
    public void setUp() {
        this.peerTable = PeerTestUtils.createPeerTable();
        // owner added already
        assert peerTable.getAllPeersFromBucketsOf().size() == 1;

    }

    @Test
    public void getLatestPeers() {
        SlowTest.apply();
        assert peerTable.getStoreCount() == 0;

        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer1);
        assert peerTable.getStoreCount() == 1;

        Utils.sleep(2000);

        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");
        peerTable.addPeer(peer2);
        assert peerTable.getStoreCount() == 2;

        long touchedTime = peer2.getModified();
        List<Peer> latestPeerList = peerTable.getLatestPeers(touchedTime);

        assertEquals(latestPeerList.size(), 1);
        assertTrue(!latestPeerList.contains(peer1));
        assertTrue(latestPeerList.contains(peer2));
    }

    @Test
    public void getPeersTest() {
        // acct
        Peer requester = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Collection<String> peerListWithoutRequester = peerTable.getPeers(requester);

        // assert
        assert peerListWithoutRequester.size() == 1;
        assert peerTable.getPeerUriList().contains(requester.getYnodeUri());
    }

    @Test
    public void getBootstrappingSeedList() {
        assert peerTable.getBootstrappingSeedList().size() == 1;
    }

    @Test
    public void getClosestPeers() {
        assert peerTable.getClosestPeers(
                peerTable.getOwner(), KademliaOptions.BUCKET_SIZE).size() == 0;

        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:22909");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32830");
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:31340");
        Peer peer4 = Peer.valueOf("ynode://75bff16c@127.0.0.1:20750");

        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        peerTable.addPeer(peer3);
        peerTable.addPeer(peer4);

        assertTrue(peerTable.getClosestPeers(peer2, 1).contains(peer2));
    }

    @Test
    public void touchPeer() {
        peerTable.touchPeer(peerTable.getOwner());
    }

    @Test
    public void copyLiveNode() {
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);

        assertEquals(peerTable.getBucketsCount(), 3);
        assertEquals(peerTable.getStoreCount(), 0);

        Utils.sleep(200);

        peerTable.copyLiveNode(100);

        assertEquals(peerTable.getBucketsCount(), 3);
        assertEquals(peerTable.getStoreCount(), 3);

        peerTable.copyLiveNode(300);

        assertEquals(peerTable.getBucketsCount(), 3);
        assertEquals(peerTable.getStoreCount(), 3);
    }
}