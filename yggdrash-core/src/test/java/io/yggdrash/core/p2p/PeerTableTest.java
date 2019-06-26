package io.yggdrash.core.p2p;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.util.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PeerTableTest {
    private KademliaPeerTable peerTable;

    @Before
    public void setUp() {
        this.peerTable = PeerTestUtils.createTable();
        assert peerTable.getPeerUriList().isEmpty();
    }

    @Test
    public void dropPeer() {
        // arrange
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:33001");
        peerTable.addPeer(peer);
        assert peerTable.contains(peer);

        // act
        peerTable.dropPeer(peer);

        // assert
        assertFalse(peerTable.contains(peer));
    }

    @Test
    public void shouldReturnNullRevalidate() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:33002");
        peerTable.addPeer(peer);

        // act
        Peer peerForRevalidate = peerTable.peerToRevalidate();

        // assert
        assertNull(peerForRevalidate);
    }

    @Test
    public void shouldReturnRevalidate() {
        // arrange
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:33003");
        peerTable.addPeer(peer);
        PeerBucket peerBucket = peerTable.getBucketByPeer(peer);
        peerBucket.getReplacements().add(Peer.valueOf("ynode://75bff16c@127.0.0.1:33004"));

        // act
        Peer peerForRevalidate = peerTable.peerToRevalidate();

        // assert
        assertEquals(peer, peerForRevalidate);
    }

    @Test
    public void getLatestPeers() {
        SlowTest.apply();

        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33005");
        peerTable.addPeer(peer1);

        Utils.sleep(2000);

        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33006");
        peerTable.addPeer(peer2);

        long touchedTime = peer2.getModified();
        List<Peer> latestPeerList = peerTable.getLatestPeers(touchedTime);

        assertEquals(1, latestPeerList.size());
        assertFalse(latestPeerList.contains(peer1));
        assertTrue(latestPeerList.contains(peer2));
    }

    @Test
    public void getClosestPeers() {
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33007");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33008");

        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);

        assertTrue(peerTable.getClosestPeers(peer1, 1).contains(peer1));
    }

    @Test
    public void getBucketByPeer() {
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33009");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33010");
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        assertEquals(peerTable.getBucketByPeer(peer1).getDepth(), peerTable.getBucketByPeer(peer2).getDepth());
    }

    @Test
    public void copyLiveNode() {
        TestConstants.SlowTest.apply();
        // arrange(1)
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33011");
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33012");
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        assertEquals(0, peerTable.getPeerStore().size());

        // act
        Utils.sleep(10);
        peerTable.copyLivePeer(5);

        // assert
        assertEquals(0, peerTable.getPeerStore().size());

        // act
        peerTable.copyLivePeer(500);
        // assert
        assertEquals(2, peerTable.getPeerStore().size());

        // arrange(2)
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33013");
        Peer peer4 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33014");
        Peer peer5 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33015");
        peerTable.addPeer(peer3);
        peerTable.addPeer(peer4);
        peerTable.addPeer(peer5);
        assertEquals(2, peerTable.getPeerStore().size());

        // act
        Utils.sleep(10);
        peerTable.copyLivePeer(15);

        //assert
        assertEquals(3, peerTable.getPeerStore().size());
    }

    /**
     * 이 테스트는, 아래 피어들이 동일한 피어 버킷에 담기는 환경에 대한 테스트 입니다.
     * 버킷사이즈가 초과되면 피어의 거리에 따라 대체되는 결과 확인을 목적으로 합니다.
     * 아래 피어 아이디는 고정이며, 피어 아이디가 변경되면 버킷 아이디가 변경되어 테스트 결과가 달라질 수 있습니다.
     * <p>
     * This test is a test for the environment in which the following peers are placed in the same peer bucket.
     * If the bucket size is exceeded, it will be replaced by the peer's distance.
     * The below peer ID is fixed.
     * The bucket ID is changed and the test result may be changed if the peer ID is changed.
     */
    @Test
    public void pickReplacement() {
        KademliaOptions.BUCKET_SIZE = 2;

        // 32920 is the owner of the peerTable
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33016"); // bucketId => 159
        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33017"); // bucketId => 159
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:33019"); // bucketId => 159
        peerTable.addPeer(peer1);
        peerTable.addPeer(peer2);
        peerTable.addPeer(peer3); // This will be added to the replacement list of the 158th bucket

        assertEquals(2, peerTable.getPeerUriList().size());

        //peer2 is the latest peer in the bucket so it will be replaced by peer3
        assertEquals(peer3, peerTable.pickReplacement(peer2));
        assertEquals(2, peerTable.getPeerUriList().size());
        assertEquals(peerTable.getBucketByIndex(159), peerTable.getBucketByPeer(peer1));
        assertEquals(2, peerTable.getBucketIdAndPeerList().get(159).size());
    }
}