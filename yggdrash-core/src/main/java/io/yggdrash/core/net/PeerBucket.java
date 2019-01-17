package io.yggdrash.core.net;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PeerBucket {
    private final int depth;
    private final Set<Peer> peers = new HashSet<>();

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
        // updated
        peers.remove(p);
        peers.add(p);
        return null;
    }

    private Peer getLastSeen() {
        List<Peer> sorted = new ArrayList<>(peers);
        sorted.sort(new TimeComparator());
        return sorted.get(0);
    }

    synchronized void dropPeer(Peer peer) {
        peers.remove(peer);
    }

    int getPeersCount() {
        return peers.size();
    }

    Set<Peer> getPeers() {
        return peers;
    }

    Peer findByPeer(Peer p) {
        if (peers.contains(p)) {
            for (Peer saved : peers) {
                if (saved.equals(p)) {
                    return saved;
                }
            }
        }
        return null;
    }
}
