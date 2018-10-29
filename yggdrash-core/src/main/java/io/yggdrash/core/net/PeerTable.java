package io.yggdrash.core.net;

import io.yggdrash.core.store.PeerStore;

import java.util.ArrayList;
import java.util.List;

public class PeerTable {
    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient PeerStore peerStore;

    PeerTable(PeerStore peerStore, Peer p) {
        this(peerStore, p, true);
    }

    private PeerTable(PeerStore peerStore, Peer p, boolean includeHomeNode) {
        this.owner = p;
        this.peerStore = peerStore;
        init();
        if (includeHomeNode) {
            addPeer(this.owner);
        }
    }

    public Peer getPeer() {
        return owner;
    }

    public final void init() {
        buckets = new PeerBucket[KademliaOptions.BINS];
        for (int i = 0; i < KademliaOptions.BINS; i++) {
            buckets[i] = new PeerBucket(i);
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
        return peerStore.getAll().size();
    }

    synchronized List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();

        for (PeerBucket b : buckets) {
            for (Peer p : b.getPeers()) {
                if (!p.equals(owner)) {
                    peers.add(p);
                }
            }
        }
        return peers;
    }

    synchronized List<Peer> getClosestPeers(byte[] targetId) {
        List<Peer> closestEntries = getAllPeers();
        closestEntries.sort(new DistanceComparator(targetId));
        if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
            closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
        }

        return new ArrayList<>(closestEntries);
    }

    synchronized boolean isPeerStoreEmpty() {
        return peerStore.isEmpty();
    }

    synchronized List<String> getAllFromPeerStore() {
        return peerStore.getAll();
    }
}