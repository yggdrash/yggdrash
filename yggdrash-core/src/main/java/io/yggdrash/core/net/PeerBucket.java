package io.yggdrash.core.net;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PeerBucket {
    private final int depth;
    private final int maxReplacements = 10; // Size of per-bucket replacement list
    private final Set<Peer> peers = new LinkedHashSet<>();
    private final Set<Peer> replacements = new LinkedHashSet<>();

    PeerBucket(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public int getPeersCount() {
        return peers.size();
    }

    public Peer getLastPeer() {
        return peers.stream().skip(getPeersCount() - 1).findFirst().orElse(null);
    }

    public synchronized void dropPeer(Peer peer) {
        peers.remove(peer);
    }

    // bump moves the given peer to the front of the bucket entry list
    // if it contained in that list
    public synchronized void bump(Peer peer) {
        if (peers.contains(peer)) {
            moveToFront(peers, peer);
        }
    }

    synchronized void addPeer(Peer peer) {
        /*
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
        */
        if (!bumpOrAdd(peer)) {
            addReplacement(peer);
        }
    }

    // bumpOrAdd moves peer to the front of the bucket entry list or adds it
    // if the list isn't full. The return value is true if peer is in the bucket.
    synchronized boolean bumpOrAdd(Peer peer) {
        if (peers.contains(peer)) {
            bump(peer);
            return true;
        }

        if (peers.size() >= KademliaOptions.BUCKET_SIZE) {
            return false;
        }

        peers.add(peer);
        // TODO delete peer from the replacements
        deleteReplacement(peer);
        peer.touch();
        return true;
    }


    private synchronized void addReplacement(Peer peer) {
        if (replacements.size() < maxReplacements) {
            replacements.add(peer);
        }

        // adds peer to the front of the replacements, keeping at most max items.
        moveToFront(replacements, peer);
    }

    private synchronized void deleteReplacement(Peer peer) {
        replacements.remove(peer);
    }

    private synchronized void moveToFront(Set<Peer> list, Peer peer) {
        // move it to the front
        Set<Peer> dup = new LinkedHashSet<>();
        dup.add(peer);
        dup.addAll(list);
        list.clear();
        list.addAll(dup);
    }

    private Peer getLastSeen() {
        List<Peer> sorted = new ArrayList<>(peers);
        sorted.sort(new TimeComparator());
        return sorted.get(0);
    }

    Set<Peer> getPeers() {
        return peers;
    }

    Set<Peer> getReplacements() {
        return replacements;
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
