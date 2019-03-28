package io.yggdrash.core.p2p;

import com.google.common.annotations.VisibleForTesting;

import java.util.LinkedHashSet;
import java.util.Set;

public class PeerBucket {
    private static final int MAX_REPLACEMENT = 10; // Size of per-bucket replacement list
    private final int depth;
    private final Set<Peer> peers = new LinkedHashSet<>();
    private final Set<Peer> replacements = new LinkedHashSet<>();

    PeerBucket(int depth) {
        this.depth = depth;
    }

    int getDepth() {
        return depth;
    }

    public int getPeersCount() {
        return peers.size();
    }

    Peer getLastPeer() {
        return lastPeerOf(peers);
    }

    private Peer lastPeerOf(Set<Peer> list) {
        return list.stream().skip(list.size() - 1L).findFirst().orElse(null);
    }

    synchronized void dropPeer(Peer peer) {
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
        if (!bumpOrAdd(peer)) {
            addReplacement(peer);
        }
    }

    // bumpOrAdd moves peer to the front of the bucket entry list or adds it
    // if the list isn't full. The return value is true if peer is in the bucket.
    private synchronized boolean bumpOrAdd(Peer peer) {
        if (peers.contains(peer)) {
            bump(peer);
            return true;
        }

        if (peers.size() >= KademliaOptions.BUCKET_SIZE) {
            return false;
        }

        peers.add(peer);
        deleteReplacement(peer);
        peer.touch();
        return true;
    }

    // replace removes peer from the replacement list and replaces 'last' with it if it is the
    // last entry in the bucket. If 'last' isn't the last entry, it has either been replaced
    // with someone else or became active.
    synchronized Peer replace(Peer last) {
        if (peers.isEmpty() || getLastPeer() != last) {
            // Entry has moved, don't replace it
            return null;
        }

        // Still the last entry
        if (replacements.isEmpty()) {
            dropPeer(last);
            return null;
        }

        // replaced dead node
        Peer peerToBeReplaced = lastPeerOf(replacements);
        deleteReplacement(peerToBeReplaced);
        dropPeer(last);
        addPeer(peerToBeReplaced);
        return peerToBeReplaced;
    }

    private synchronized void addReplacement(Peer peer) {
        if (replacements.size() < MAX_REPLACEMENT) {
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

    Set<Peer> getPeers() {
        return peers;
    }

    @VisibleForTesting
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
