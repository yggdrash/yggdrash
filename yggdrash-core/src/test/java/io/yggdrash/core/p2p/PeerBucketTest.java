package io.yggdrash.core.p2p;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PeerBucketTest {
    private final Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
    private final Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");

    private PeerBucket bucket;

    @Before
    public void setUp() {
        bucket = new PeerBucket(1);
        assert bucket.getDepth() == 1;
    }

    @Test
    public void testDefaultSizeBucket() {

        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer2);
        assert bucket.getPeersCount() == 2;
        bucket.dropPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.dropPeer(peer2);
        assertEquals(0, bucket.getPeersCount());
    }

    @Test
    public void testMaxBucketSize() {

        KademliaOptions.BUCKET_SIZE = 1;
        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer2);
        assert bucket.getPeersCount() == 1;
        bucket.dropPeer(peer1);
        assertEquals(0, bucket.getPeersCount());
    }

    @Test
    public void testAddDuplicatedPeer() {

        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer1);
        assertEquals(1, bucket.getPeersCount());
    }

    /*
    bump 는 현재 버킷의 엔트리(peerBucket) 의 맨 앞으로 이동시킨다.
    p1, p2, p3를 추가 시킨 후 p3 을 bump 시키면 p3가 엔트리의 처음으로 이동한다.
     */
    @Test
    public void testBump() {    // moveToFront
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32923");

        bucket.addPeer(peer1); // [0] -> [1]
        bucket.addPeer(peer2); // [1] -> [2]
        bucket.addPeer(peer3); // [2] -> [0]

        bucket.bump(peer3);

        assert bucket.getPeersCount() == 3;
        assertTrue(getLastPeer(peer2));
    }

    private boolean getLastPeer(Peer peer) {
        return bucket.getLastPeer().equals(peer);
    }

    /*
    addPeer 시 BUCKET_SIZE 가 넘어가면 replacement 에 peer 를 bump 하면서 추가
    peers 배열 처음이 가장 처음에 추가된 peer 이고,
    replacements 배역 마지막이 가장 처음에 추가됨 peer 이다. (replace 대상)
    */
    @Test
    public void testAddPeer() {
        KademliaOptions.BUCKET_SIZE = 5;

        addPeerToBucket(32920, 32930);

        assertEquals(5, bucket.getPeersCount());
        assertEquals(5, bucket.getReplacements().size());
        assertTrue(getLastPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32924)));
        Peer lastPeerOfReplacements = bucket.getReplacements()
                .stream()
                .skip(bucket.getReplacements().size() - 1)
                .findFirst()
                .orElse(null);
        assertEquals(lastPeerOfReplacements, Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32925));
    }

    /*
    replace 는 peerTask 에서 (peer 가 존재하는)랜덤 피어 버킷의 last peer 를 뽑아 healthCheck 를
    통과하지 못하면 해당 피어의 피어버킷에 replacement 리스트가 존재하는 경우 리스트의 peer 와 교체한다.
    */
    @Test
    public void testReplace() {
        KademliaOptions.BUCKET_SIZE = 5;

        // 5개의 피어를 버킷에 추가한다. 현재 replacement 리스트에는 피어가 없다.
        // 추가된 5개의 피어가 아닌 다른 피어를 replace 시킨다.
        // 결과 => return 값은 null
        //        replace 되지 않는다.
        //        피어 버킷의 사이즈는 5이다.
        addPeerToBucket(32920, 32925);
        Peer randomPeer = Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 9999);

        assertNull(bucket.replace(randomPeer));
        assertEquals(5, bucket.getPeersCount());
        assertTrue(getLastPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32924)));

        // 마지막 피어가 healthCheck 에 실패했다고 가정하고 replace 시킨다.
        // 결과 => return 값은 null
        //        replacement 리스트 사이즈가 0이므로 그냥 drop 만 시킨다.
        //        피어 버킷의 사이즈는 4이다.
        assertNull(bucket.replace(bucket.getLastPeer()));
        assertEquals(4, bucket.getPeersCount());
        assertTrue(getLastPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32923)));

        // 6개의 피어를 피어버킷에 추가한다. 5개는 replacement 리스트에 추가된다.
        // 마지막 피어가 healthCheck 에 실패했다고 가정하고 replace 시킨다.
        // 결과 => return 값은 6번째 피어
        //        replacement 리스트의 마지막피어인 6번째 피어와 5번째 피어가 교체된다.
        //        피어 버킷의 사이즈는 5이며, 마지막 피어는 6번째 피어이다.
        //        replacement 리스트의 마지막 피어는 7번째 피어이고 사이즈는 4이다.
        addPeerToBucket(32924, 32930);
        assertEquals(5, bucket.getReplacements().size());
        assertEquals(bucket.replace(bucket.getLastPeer()),
                Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32925));

        printList(bucket.getReplacements(), "replace");
        printList(bucket.getPeers(), "p");

        assertEquals(4, bucket.getReplacements().size());
        assertTrue(getLastPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:" + 32925)));
    }

    private void addPeerToBucket(int startPort, int endPort) {
        for (int port = startPort; port < endPort; port++) {
            Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:" + port);
            bucket.addPeer(peer);
            printList(bucket.getPeers(), "bucket");
            printList(bucket.getReplacements(), "replacement");
        }
    }

    private void printList(Set<Peer> peerList, String nameOfList) {
        System.out.println("\n");
        int i = 0;
        for (Peer p : peerList) {
            System.out.println("[" + i + "] " + nameOfList + " peer => " + p);
            i++;
        }
    }
}