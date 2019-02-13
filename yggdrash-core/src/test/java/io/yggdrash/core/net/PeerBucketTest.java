package io.yggdrash.core.net;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public void bump() {
        Peer peer3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32923");

        bucket.addPeer(peer1);
        bucket.addPeer(peer2);
        bucket.addPeer(peer3);

        bucket.bump(peer3);

        assert bucket.getPeersCount() == 3;
        assert getLastPeer(peer2);
    }

    private boolean getLastPeer(Peer peer) {
        return bucket.getLastPeer().getPeerId() == peer.getPeerId();
    }

    @Test
    public void addPeer() {
        List<Peer> tmp = new ArrayList<>();
        for (int i = 32920; i < 32940; i++) {
            Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:" + i);
            tmp.add(peer);

            bucket.addPeer(peer);
        }

        int addedPeerCnt = tmp.size();
        bucket.addPeer(tmp.get(addedPeerCnt - 5));               //35

        Set<Peer> replacements = bucket.getReplacements();

        assert getLastPeer(tmp.get(addedPeerCnt - 6));
        assert replacements.size() == 4;
        assert replacements.contains(tmp.get(addedPeerCnt - 4)); //36
        assert replacements.contains(tmp.get(addedPeerCnt - 3)); //37
        assert replacements.contains(tmp.get(addedPeerCnt - 2)); //38
        assert replacements.contains(tmp.get(addedPeerCnt - 1)); //39
    }
}