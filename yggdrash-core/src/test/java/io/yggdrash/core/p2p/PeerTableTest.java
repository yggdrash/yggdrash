package io.yggdrash.core.p2p;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.util.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PeerTableTest {
    private KademliaPeerTable peerTable;

    @Before
    public void setUp() {
        this.peerTable = PeerTestUtils.createTable();
        assert peerTable.getPeerUriList().isEmpty();
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
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");

        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);

        assertTrue(peerTable.getClosestPeers(peer1, 1).contains(peer1));
    }

    @Test
    public void getBucketByPeer() {
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        assertEquals(peerTable.getBucketByPeer(peer1).getDepth(), peerTable.getBucketByPeer(peer2).getDepth());
    }

    @Test
    public void copyLiveNode() {

        // arrange
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        assertEquals(0, peerTable.getPeerStore().size());

        // act
        Utils.sleep(10);
        peerTable.copyLiveNode(5);
        // assert
        assertEquals(peerTable.getPeerStore().size(), 0);

        // act
        peerTable.copyLiveNode(500);
        // assert
        assertEquals(peerTable.getPeerStore().size(), 2);
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

        assertEquals(2, peerTable.getPeerUriList().size());

        //peer2 is the latest peer in the bucket so it will be replaced by peer3
        assertEquals(peer3, peerTable.pickReplacement(peer2));
        assertEquals(2, peerTable.getPeerUriList().size());
        assertEquals(peerTable.getBucketByIndex(158), peerTable.getBucketByPeer(peer1));
        assertEquals(2, peerTable.getBucketIdAndPeerList().get(158).size());
    }
}