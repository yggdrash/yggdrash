package io.yggdrash.core.net;

import java.util.ArrayList;
import java.util.List;

public class PeerTable {
    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient List<Peer> peers;

    PeerTable(Peer p) {
        this(p, true);
    }

    private PeerTable(Peer p, boolean includeHomeNode) {
        this.owner = p;
        init();
        if (includeHomeNode) {
            addPeer(this.owner);
        }
    }

    public Peer getPeer() {
        return owner;
    }

    public final void init() {
        peers = new ArrayList<>();
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
        if (!peers.contains(p)) {
            peers.add(p);
        }
        return null;
    }

    synchronized void dropPeer(Peer p) {
        buckets[getBucketId(p)].dropPeer(p);
        peers.remove(p);
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
        return peers.size();
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
}