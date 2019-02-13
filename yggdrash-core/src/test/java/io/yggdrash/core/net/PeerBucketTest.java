package io.yggdrash.core.net;

import io.yggdrash.TestConstants;
import org.junit.Before;
import org.junit.Test;

public class PeerBucketTest {
    private Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
    private Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");

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
        assert bucket.getPeersCount() == 0;
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
        assert bucket.getPeersCount() == 0;
    }

    @Test
    public void testAddDuplicatedPeer() {

        assert bucket.getPeersCount() == 0;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
        bucket.addPeer(peer1);
        assert bucket.getPeersCount() == 1;
    }

    @Test
    public void testUpdatedWithLatestPeer() {
        // arrange
        Peer newPeerWithBestBlock = Peer.valueOf(peer1.getYnodeUri());
        newPeerWithBestBlock.updateBestBlock(BestBlock.of(TestConstants.yggdrash(), 0));

        bucket.addPeer(peer1);
        assert bucket.findByPeer(peer1).getBestBlocks().size() == 0;

        // act
        bucket.addPeer(newPeerWithBestBlock);

        // assert
        assert bucket.getPeers().size() == 1;
        assert bucket.findByPeer(peer1).getBestBlocks().size() == 1;
    }
}