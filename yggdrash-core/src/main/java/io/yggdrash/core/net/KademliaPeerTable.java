package io.yggdrash.core.net;

import com.google.common.annotations.VisibleForTesting;
import io.yggdrash.core.store.PeerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class KademliaPeerTable implements PeerTable {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerTable.class);

    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient PeerStore peerStore;

    public KademliaPeerTable(Peer owner, PeerStore peerStore) {
        this.owner = owner;
        this.peerStore = peerStore;
        init();
    }

    private void init() {
        buckets = new PeerBucket[KademliaOptions.BINS];
        for (int i = 0; i < KademliaOptions.BINS; i++) {
            buckets[i] = new PeerBucket(i);
        }
    }

    @Override
    public void loadSeedNodes(List<String> seedPeerList) {
        if (this.peerStore.size() > 0) {
            this.peerStore.getAll().forEach(Peer::valueOf);
        }

        // Load nodes from the database and insert them.
        // This should yield a few previously seen nodes that are (hopefully) still alive.
        if (getBucketsCount() < 1 && seedPeerList != null) {
            seedPeerList.stream().map(Peer::valueOf).forEach(peer -> {
                if (!owner.toAddress().equals(peer.toAddress())) {
                    addPeer(peer);
                }
            });
        }
    }

    // resolve searches for a specific peer with the given ID.
    // It returns null if the node could not be found.
    Peer resolve(Peer peer) {
        // If the node is present in the local table, no network interaction is required.
        List<Peer> closest = getClosestPeers(peer, 1);
        if (closest.size() > 0 && closest.contains(peer)) {
            return peer;
        }

        // Otherwise, do a network lookup (TODO network lookup implementation)
        // (The current network lookup is the dht task of kademliaDiscovery.)
        // Set<Peer> res = lookup(peer);
        // if (res.contains(peer)) { return peer; }

        return null;
    }

    // addPeer attempts to add the given peer to its corresponding bucket.
    // If the bucket has space available, adding the peer succeeds immediately.
    // Otherwise, the node is added if the least recently active node in the bucket
    // does not respond to a ping packet. (TODO implementation of healthCheck by ping)
    @Override
    public synchronized void addPeer(Peer peer) {
        peer.setDistance(owner);
        buckets[getBucketId(peer)].addPeer(peer);

        log.trace("peerTable :: addPeer => {}, peersCnt => {}, bucketSize => {}",
                peer.toAddress(), getAllPeers().size(), getBucketsCount());
    }

    public synchronized boolean contains(Peer p) {
        for (PeerBucket b : buckets) {
            if (b.getPeers().contains(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void copyLiveNode(long minTableTime) {
        long baseTime = System.currentTimeMillis();
        for (Peer peer : getAllPeers()) {
            if (baseTime - peer.getModified() < minTableTime) {
                updatePeerStore(peer);
            }
        }

    }

    private void updatePeerStore(Peer peer) {
        if (!peerStore.contains(peer.getPeerId())) {
            // TODO overwrite peer which should be updated in the db
            peerStore.put(peer.getPeerId(), peer);
            log.debug("Added peerStore size={}, peer={}", peerStore.size(), peer.toAddress());
        }
    }

    @Override
    public synchronized Peer pickReplacement(Peer peer) {
        return getBucketByPeer(peer).replace(peer);
    }

    @Override
    public synchronized PeerBucket getBucketByIndex(int i) {
        return buckets[i];
    }

    @Override
    public synchronized PeerBucket getBucketByPeer(Peer p) {
        return buckets[getBucketId(p)];
    }

    @Override
    public synchronized int getBucketsCount() {
        int i = 0;
        for (PeerBucket b : buckets) {
            if (b.getPeersCount() > 0) {
                i++;
            }
        }
        return i;
    }

    @Override
    public List<Peer> getLatestPeers(long reqTime) {
        long limitTime = reqTime - 1000;
        List<Peer> latestPeers = new ArrayList<>();

        for (PeerBucket b : buckets) {
            b.getPeers().forEach(peer -> {
                if (peer.getModified() >= limitTime) {
                    latestPeers.add(peer);
                }
            });
        }

        return latestPeers;
    }

    // This function is for gateway-node.
    @Override
    public Map<Integer, List<Peer>> getBucketIdAndPeerList() {
        Map<Integer, List<Peer>> res = new LinkedHashMap<>();
        if (getBucketsCount() > 0) {
            int i = 0;
            for (PeerBucket b : buckets) {
                if (b.getPeersCount() > 0) {
                    res.put(i, new ArrayList<>(b.getPeers()));
                }
                i++;
            }
        }
        return res;
    }

    private int getBucketId(Peer p) {
        int id = p.getDistance() - 1;
        return id < 0 ? 0 : id;
    }

    private synchronized List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();

        for (PeerBucket b : buckets) {
            peers.addAll(b.getPeers());
        }
        return peers;
    }

    @Override
    public List<String> getPeerUriList() {
        return getAllPeers().stream()
                .map(Peer::getYnodeUri).collect(Collectors.toList());
    }

    // This function is for gateway-node.
    public List<String> getAllPeerAddressList() {
        return getAllPeers()
                .stream()
                .map(p -> String.format("%s:%d", p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Peer> getClosestPeers(Peer targetPeer, int limit) {
        List<Peer> closestEntries = getAllPeers();
        closestEntries.sort(new DistanceComparator(targetPeer.getPeerId().getBytes()));
        if (closestEntries.size() > limit) {
            closestEntries = closestEntries.subList(0, limit);
        }

        return closestEntries;
    }

    /**
     * call back from PeerDialer
     * @param peer disconnected peer
     */
    @Override
    public void dropPeer(Peer peer) {
        buckets[getBucketId(peer)].dropPeer(peer);
        peerStore.remove(peer.getPeerId());
    }


    // returns the last node in a random, non-empty bucket
    @Override
    public Peer peerToRevalidate() {
        Random r = new Random();
        int cnt = 1;
        int startIndex = r.nextInt(KademliaOptions.BINS);

        for (; cnt < KademliaOptions.BINS; startIndex++, cnt++) {
            if (startIndex == 0 || startIndex == KademliaOptions.BINS) {
                startIndex = 1;
            }

            //log.debug("peerTask :: bucketIndex => " + startIndex);

            PeerBucket bucket = getBucketByIndex(startIndex);

            if (!bucket.getReplacements().isEmpty()) {
                return bucket.getLastPeer();
            }
        }
        return null;
    }

    @VisibleForTesting
    public PeerBucket[] getBuckets() {
        return buckets;
    }

    @VisibleForTesting
    PeerStore getPeerStore() {
        return peerStore;
    }
}