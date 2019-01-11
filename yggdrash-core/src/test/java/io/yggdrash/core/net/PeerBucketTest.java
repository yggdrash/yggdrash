package io.yggdrash.core.net;

import org.junit.Test;

public class PeerBucketTest {
    private Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
    private Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");

    @Test
    public void testDefaultSizeBucket() {
        PeerBucket bucket = new PeerBucket(1);
        assert bucket.getDepth() == 1;

        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer2);
        assert bucket.getPeersCount() == 2;
        bucket.dropPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.dropPeer(peer2);
        assert bucket.getPeersCount() == 0;
    }

    @Test
    public void testMaxBucketSize() {
        PeerBucket bucket = new PeerBucket(2);
        assert bucket.getDepth() == 2;

        KademliaOptions.BUCKET_SIZE = 1;
        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer2);
        assert bucket.getPeersCount() == 1;
        bucket.dropPeer(peer1);
        assert bucket.getPeersCount() == 0;
    }
}