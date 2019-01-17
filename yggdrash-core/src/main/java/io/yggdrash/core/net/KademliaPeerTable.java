package io.yggdrash.core.net;

import io.yggdrash.core.store.PeerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KademliaPeerTable implements PeerTable {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerTable.class);

    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient PeerStore peerStore;
    private List<String> seedPeerList;

    public KademliaPeerTable(Peer owner, PeerStore peerStore) {
        this.owner = owner;
        this.peerStore = peerStore;
        init();
        addPeer(owner);
    }

    private void init() {
        buckets = new PeerBucket[KademliaOptions.BINS];
        for (int i = 0; i < KademliaOptions.BINS; i++) {
            buckets[i] = new PeerBucket(i);
        }

        if (this.peerStore.size() > 0) {
            this.peerStore.getAll().forEach(s -> addPeer(Peer.valueOf(s)));
        }
    }

    public Peer getOwner() {
        return owner;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    @Override
    public List<String> getBootstrappingSeedList() {
        List<String> seedPeerList;

        if (peerStore.size() > 1) {
            seedPeerList = getAllFromPeerStore();
        } else {
            seedPeerList = this.seedPeerList;
        }

        if (seedPeerList == null || seedPeerList.isEmpty()) {
            return new ArrayList<>();
        }

        return seedPeerList;
    }

    @Override
    public synchronized Peer addPeer(Peer p) {
        p.setDistance(owner);
        Peer lastSeen = buckets[getBucketId(p)].addPeer(p);
        if (lastSeen != null) {
            return lastSeen;
        }
        if (!peerStore.contains(p.getPeerId())) {
            peerStore.put(p.getPeerId(), p);
            log.debug("Added peerStore size={}, peer={}", count(), p.toAddress());
        }
        return null;
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
    public synchronized void touchPeer(Peer p) {
        if (!contains(p)) {
            return;
        }
        for (PeerBucket b : buckets) {
            Peer found = b.findByPeer(p);
            if (found != null) {
                found.touch();
                break;
            }
        }
    }

    private int getBucketsCount() {
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

    @Override
    public synchronized int count() {
        return peerStore.size();
    }

    private synchronized List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();

        for (PeerBucket b : buckets) {
            peers.addAll(b.getPeers());
        }
        return peers;
    }

    public List<String> getPeers(Peer peer) {

        List<String> peerList = getPeerUriList();
        addPeer(peer);
        return peerList;
    }

    @Override
    public List<String> getPeerUriList() {
        return getAllPeers().stream()
                .map(Peer::getYnodeUri).collect(Collectors.toList());
    }

    public List<String> getAllPeersFromBucketsOf() {
        return getAllPeers()
                .stream()
                .map(p -> String.format("%s:%d", p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Peer> getClosestPeers() {
        List<Peer> closestEntries = getAllPeers();
        closestEntries.remove(owner);
        closestEntries.sort(new DistanceComparator(owner.getPeerId().getBytes()));
        if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
            closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
        }

        return closestEntries;
    }

    private synchronized List<String> getAllFromPeerStore() {
        return peerStore.getAll();
    }

    /**
     * call back from PeerHandlerGroup
     * @param peer disconnected peer
     */
    @Override
    public void peerDisconnected(Peer peer) {
        buckets[getBucketId(peer)].dropPeer(peer);
        peerStore.remove(peer.getPeerId());
    }
}