package io.yggdrash.core.net;

import java.util.ArrayList;
import java.util.List;

public class PeerBucket {
    private final int depth;
    private final List<Peer> peers = new ArrayList<>();

    PeerBucket(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    synchronized Peer addPeer(Peer p) {
        if (!peers.contains(p)) {
            if (peers.size() >= KademliaOptions.BUCKET_SIZE) {
                return getLastSeen();
            } else {
                peers.add(p);
            }
        }
        return null;
    }

    private Peer getLastSeen() {
        List<Peer> sorted = peers;
        sorted.sort(new TimeComparator());
        return sorted.get(0);
    }

    synchronized void dropPeer(Peer peer) {
        peers.remove(peer);
    }

    int getPeersCount() {
        return peers.size();
    }

    List<Peer> getPeers() {
        return peers;
    }
}
