package io.yggdrash.core.net;

import io.yggdrash.core.store.PeerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KademliaPeerTable implements PeerTable, Dht {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerTable.class);

    private final Peer owner;  // our node
    private transient PeerBucket[] buckets;
    private transient PeerStore peerStore;
    private final PeerHandlerFactory factory;
    private List<String> seedPeerList;

    public KademliaPeerTable(Peer owner, PeerStore peerStore, PeerHandlerFactory factory) {
        this.owner = owner;
        this.peerStore = peerStore;
        this.factory = factory;
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

    @Override
    public void refresh(Peer target) {
        if (target == null) {
            return;
        }

        int size = getClosestPeers(target, 1).size();
        log.debug("peerTable :: refresh =>  target = {}, size = {}", target.getYnodeUri(), size);
        if (size < 1) {
            // The result set is empty, all peers were dropped, discover.
            // We actually wait for the discover to complete here.
            // The very first query will hit this case and run the bootstrapping logic.
            selfRefresh();
        }
        lookup(0, new ArrayList<>(), target);
    }

    @Override
    public void selfRefresh() {
        loadSeedNodes();
        lookup(0, new ArrayList<>(), getOwner());
    }

    private synchronized void lookup(int round, List<Peer> prevTried, Peer target) {
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", getBucketsCount()), getPeerUriList());
                return;
            }

            List<Peer> closest = getClosestPeers(target, KademliaOptions.BUCKET_SIZE);
            List<Peer> tried = new ArrayList<>();

            for (Peer peer : closest) {
                if (!tried.contains(peer) && !prevTried.contains(peer)) {
                    PeerHandler peerHandler = factory.create(peer);
                    try {
                        List<Peer> peerList = peerHandler.findPeers(target);
                        peerList.forEach(this::addPeer);
                        tried.add(peer);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    } finally {
                        peerHandler.stop();
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

            if (tried.isEmpty()) {
                log.debug("Terminating discover after {} rounds.", round);
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", getBucketsCount()), getPeerUriList());
                return;
            }

            tried.addAll(prevTried);
            lookup(round + 1, tried, target);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }

    public void loadSeedNodes() {
        // Load nodes from the database and insert them.
        // This should yield a few previously seen nodes that are (hopefully) still alive.
        if (getBucketsCount() < 1) {
            this.seedPeerList.stream().map(Peer::valueOf).forEach(this::addPeer);
        }
    }

    public Peer getOwner() {
        return owner;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    /*
    @Override
    public List<Peer> getBootstrappingSeedList() {
        // Load nodes from the database and insert them.
        // This should yield a few previously seen nodes that are (hopefully) still alive.
        List<Peer> seedPeerList;

        if (peerStore.size() > 1) {
            seedPeerList = getClosestPeers(owner, KademliaOptions.BUCKET_SIZE); // self -> owner == target
        } else if (this.seedPeerList != null) {
            seedPeerList = this.seedPeerList.stream().map(Peer::valueOf)
                    .collect(Collectors.toList());
        } else {
            seedPeerList = new ArrayList<>();
        }

        return seedPeerList;
    }
    */

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

        log.debug("peerTable :: addPeer => {}, bucketSize => {}",
                peer.getPeerId(), getBucketsCount());
        /*
        Peer lastSeen = buckets[getBucketId(p)].addPeer(p);
        if (lastSeen != null) {
            return lastSeen;
        }
        */

        //peer will be stored in db at specific time intervals
        //updatePeerStore(peer);
        //return null;
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
        for (Peer peer : getAllPeers()) {
            if ((System.currentTimeMillis() - peer.getModified()) > minTableTime) {
                updatePeerStore(peer);
            }
        }

    }

    private void updatePeerStore(Peer peer) {
        if (!peerStore.contains(peer.getPeerId())) {
            // TODO overwrite ??
            peerStore.put(peer.getPeerId(), peer);
            log.debug("Added peerStore size={}, peer={}", peerStore.size(), peer.toAddress());
        }
    }

    @Override
    public synchronized Peer pickReplacement(Peer peer) {
        return getBucketByPeer(peer).replace(peer);
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
        int i = -1; // exclude owner's bucket
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

    public List<String> getAllPeerAddressList() {
        return getAllPeers()
                .stream()
                .map(p -> String.format("%s:%d", p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Peer> getClosestPeers(Peer targetPeer, int limit) {
        List<Peer> closestEntries = getAllPeers();
        closestEntries.remove(owner);
        closestEntries.sort(new DistanceComparator(targetPeer.getPeerId().getBytes())); //TODO TestCode
        if (closestEntries.size() > limit) {
            closestEntries = closestEntries.subList(0, limit);
        }

        return closestEntries;
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