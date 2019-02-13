package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.util.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeerTableTest {
    private PeerTable peerTable;

    @Before
    public void setUp() {
        this.peerTable = PeerTestUtils.createTable();
        // owner added already
        assert peerTable.getAllPeerAddressList().size() == 1;
    }

    @Test
    public void getLatestPeers() {
        SlowTest.apply();

        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer1);

        Utils.sleep(2000);

        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");
        peerTable.addPeer(peer2);

        long touchedTime = peer2.getModified();
        List<Peer> latestPeerList = peerTable.getLatestPeers(touchedTime);

        assertEquals(latestPeerList.size(), 1);
        assertTrue(!latestPeerList.contains(peer1));
        assertTrue(latestPeerList.contains(peer2));
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
        assertEquals(peerTable.getPeerUriList().size(), 4); // owner included
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

        assertEquals(peerTable.getPeerUriList().size(), 3); // owner included

        Utils.sleep(200);

        peerTable.copyLiveNode(100);

        assertEquals(peerTable.getPeerUriList().size(), 3);

        peerTable.copyLiveNode(300);

        assertEquals(peerTable.getPeerUriList().size(), 3);
    }

    @Test
    public void pickReplacement() {
        KademliaOptions.BUCKET_SIZE = 2;

        // 32920 is the owner of the peerTable
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"); // bucketId => 158
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32932"); // bucketId => 158
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32942"); // bucketId => 158
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        peerTable.addPeer(peer3); // This will be added to the replacement list of the 158th bucket

        assertEquals(peerTable.getPeerUriList().size(), 3);

        //peer2 is the latest peer in the bucket so it will be replaced by peer3
        assertEquals(peerTable.pickReplacement(peer2), peer3);
        assertEquals(peerTable.getPeerUriList().size(), 3);
        assertEquals(peerTable.getBucketByIndex(158), peerTable.getBucketByPeer(peer1));
        assertEquals(peerTable.getBucketIdAndPeerList().get(158).size(), 2);
    }
}