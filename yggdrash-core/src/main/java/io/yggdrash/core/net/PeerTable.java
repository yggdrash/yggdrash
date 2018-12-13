package io.yggdrash.core.net;

import io.yggdrash.core.store.PeerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PeerTable {
    private static final Logger log = LoggerFactory.getLogger(PeerTable.class);

    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient PeerStore peerStore;

    PeerTable(PeerStore peerStore, Peer p) {
        this.owner = p;
        this.peerStore = peerStore;
        init();
        addPeer(this.owner);
    }

    public Peer getPeer() {
        return owner;
    }

    public final void init() {
        buckets = new PeerBucket[KademliaOptions.BINS];
        for (int i = 0; i < KademliaOptions.BINS; i++) {
            buckets[i] = new PeerBucket(i);
        }

        if (this.peerStore.size() > 0) {
            this.peerStore.getAll().forEach(s -> addPeer(Peer.valueOf(s)));
        }
    }

    synchronized Peer addPeer(Peer p) {
        p.setDistance(owner);
        Peer lastSeen = buckets[getBucketId(p)].addPeer(p);
        if (lastSeen != null) {
            return lastSeen;
        }
        if (!peerStore.contains(p.getPeerId())) {
            peerStore.put(p.getPeerId(), p);
            log.debug("Added size={}, peer={}", getPeersCount(), p.toAddress());
        }
        return null;
    }

    synchronized void dropPeer(Peer p) {
        buckets[getBucketId(p)].dropPeer(p);
        peerStore.remove(p.getPeerId());
    }

    public synchronized boolean contains(Peer p) {
        for (PeerBucket b : buckets) {
            if (b.getPeers().contains(p)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void touchPeer(Peer p) {
        for (PeerBucket b : buckets) {
            if (b.getPeers().contains(p)) {
                b.getPeers().get(b.getPeers().indexOf(p)).touch();
                break;
            }
        }
    }

    public int getBucketsCount() {
        int i = 0;
        for (PeerBucket b : buckets) {
            if (b.getPeersCount() > 0) {
                i++;
            }
        }
        return i;
    }

    public synchronized PeerBucket[] getBuckets() {
        return buckets;
    }

    private int getBucketId(Peer p) {
        int id = p.getDistance() - 1;
        return id < 0 ? 0 : id;
    }

    synchronized int getPeersCount() {
        return peerStore.size();
    }

    synchronized List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();

        for (PeerBucket b : buckets) {
            peers.addAll(b.getPeers());
        }
        return peers;
    }

    synchronized List<Peer> getClosestPeers(byte[] targetId) {
        List<Peer> closestEntries = getAllPeers();
        closestEntries.remove(owner);
        closestEntries.sort(new DistanceComparator(targetId));
        if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
            closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
        }

        return closestEntries;
    }

    synchronized boolean isPeerStoreEmpty() {
        return peerStore.size() == 1 && peerStore.contains(owner.getPeerId());
    }

    synchronized List<String> getAllFromPeerStore() {
        return peerStore.getAll();
    }
}