package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KademliaPeerTableGroup implements PeerTableGroup {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerTableGroup.class);

    private final Peer owner;  // our node
    private final Map<BranchId, PeerTable> tableMap = new ConcurrentHashMap<>();
    private final StoreBuilder storeBuilder;
    private final PeerDialer peerDialer;
    private List<String> seedPeerList;

    KademliaPeerTableGroup(Peer owner, StoreBuilder storeBuilder, PeerDialer peerDialer) {
        this.owner = owner;
        this.storeBuilder = storeBuilder;
        this.peerDialer = peerDialer;
    }

    @Override
    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    @Override
    public List<String> getSeedPeerList() {
        return this.seedPeerList;
    }

    @Override
    public PeerTable createTable(BranchId branchId) {
        if (tableMap.containsKey(branchId)) {
            return tableMap.get(branchId);
        }
        PeerTable peerTable = newPeerTable(branchId);
        tableMap.put(branchId, peerTable);
        return peerTable;
    }

    private PeerTable newPeerTable(BranchId branchId) {
        PeerStore peerStore = storeBuilder.setBranchId(branchId).buildPeerStore();
        return new KademliaPeerTable(owner, peerStore);
    }

    @Override
    public PeerTable getPeerTable(BranchId branchId) {
        return tableMap.containsKey(branchId) ? tableMap.get(branchId) : newPeerTable(branchId);
    }

    @Override
    public Set<BranchId> getAllBranchId() {
        return tableMap.keySet();
    }

    @Override
    public Peer getOwner() {
        return owner;
    }

    @Override
    public void addPeer(BranchId branchId, Peer peer) {
        PeerTable peerTable = tableMap.get(branchId);
        if (peerTable == null) {
            peerTable = createTable(branchId);
        }

        if (!peerTable.getPeerUriList().contains(peer.getYnodeUri())) {
            log.info("Add a peer({})", peer.getYnodeUri());
            peerTable.addPeer(peer);
        }
    }

    @Override
    public void dropPeer(BranchId branchId, Peer peer) {
        PeerTable peerTable = tableMap.get(branchId);
        if (peerTable == null) {
            return;
        }

        if (!seedPeerList.contains(peer.getYnodeUri())) {
            log.info("Delete a peer({})", peer.getYnodeUri());
            peerTable.dropPeer(peer);
        }
    }

    private boolean isNotSeed(Peer target) {
        if (seedPeerList != null) {
            for (String seed : seedPeerList) {
                if (!seed.endsWith(target.toAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized boolean contains(BranchId branchId) {
        return tableMap.containsKey(branchId);
    }

    @Override
    public Map<String, String> getActivePeerListWithStatus() {
        return peerDialer.getActivePeerListWithStatus();
    }

    @Override
    public List<Peer> getClosestPeers(BranchId branchId, Peer targetPeer, int limit) {
        if (tableMap.containsKey(branchId)) {
            List<Peer> peerList = tableMap.get(branchId).getClosestPeers(targetPeer, limit);
            log.trace("Total PeerTableMap: {}", tableMap);
            log.trace("getClosestPeers(): {}", peerList);
            return peerList;
        }
        return Collections.emptyList();
    }

    @Override
    public List<Peer> getBroadcastPeerList(BranchId branchId) {
        List<Peer> closest = getClosestPeers(branchId, owner, KademliaOptions.BROADCAST_SIZE);
        return closest.stream().collect(Collectors.toList());
    }

    @Override
    public void copyLiveNode() {
        long minTableTime = 30000;      //30 seconds
        tableMap.values().forEach(table -> table.copyLivePeer(minTableTime));
    }

    @Override
    public void selfRefresh() {
        for (Map.Entry<BranchId, PeerTable> entry : tableMap.entrySet()) {
            selfRefresh(entry);
        }
    }

    private void selfRefresh(Map.Entry<BranchId, PeerTable> entry) {
        entry.getValue().loadSeedPeers(seedPeerList);
        lookup(0, new ArrayList<>(), getOwner(), entry);
    }

    private synchronized void lookup(int round, List<Peer> prevTried, Peer target,
                                     Map.Entry<BranchId, PeerTable> entry) {
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("(MAX_STEPS) Terminating discover after {} rounds.", round);
                return;
            }

            List<Peer> tried = getPeers(prevTried, target, entry);

            if (tried.isEmpty()) {
                return;
            }

            tried.addAll(prevTried);
            lookup(round + 1, tried, target, entry);
        } catch (Exception e) {
            log.info("{}", e.getMessage());
        }
    }

    private List<Peer> getPeers(List<Peer> prevTried, Peer target, Map.Entry<BranchId, PeerTable> entry) {
        List<Peer> closest = entry.getValue().getClosestPeers(target, KademliaOptions.BUCKET_SIZE);
        List<Peer> tried = new ArrayList<>();

        for (Peer peer : closest) {
            if (!tried.contains(peer) && !prevTried.contains(peer)) {
                BlockChainHandler peerHandler = peerDialer.getPeerHandler(entry.getKey(), peer);
                findPeers(target, entry, tried, peer, peerHandler);
            }
            if (tried.size() == KademliaOptions.ALPHA) {
                break;
            }
        }
        return tried;
    }

    private void findPeers(Peer target, Map.Entry<BranchId, PeerTable> entry, List<Peer> tried, Peer peer,
                           BlockChainHandler peerHandler) {
        try {
            List<Peer> peerList = peerHandler.findPeers(entry.getKey(), target);
            for (Peer findPeer : peerList) {
                // ignore to add owner
                if (!owner.equals(findPeer)) {
                    entry.getValue().addPeer(findPeer);
                }
            }
            tried.add(peer);
        } catch (Exception e) {
            log.debug("Cannot connect to {}", peer);
            if (!seedPeerList.contains(peerHandler.getPeer().getYnodeUri())) {
                log.trace("removeHandler: {}", peerHandler.getPeer());
                peerDialer.removeHandler(peerHandler);
            }
        }
    }

    @Override
    public void refresh() {
        // The Kademlia paper specifies that the bucket refresh should perform a lookup
        // in the least recently used bucket. We cannot adhere to this because
        // the findnode target is a 512bit value (not hash-sized) and it is not easily possible
        // to generate a sha3 preimage that falls into a chosen bucket.
        // We perform a few lookups with a random target instead.
        Peer randomTarget = randomTargetGeneration();
        for (Map.Entry<BranchId, PeerTable> entry : tableMap.entrySet()) {
            int size = entry.getValue().getClosestPeers(randomTarget, 1).size();
            if (size < 1) {
                // The result set is empty, all peers were dropped, discover.
                // We actually wait for the discover to complete here.
                // The very first query will hit this case and run the bootstrapping logic.
                selfRefresh(entry);
            }
            lookup(0, new ArrayList<>(), randomTarget, entry);
        }
    }

    private Peer randomTargetGeneration() {
        // Ethereum generates 3 random pubKeys to perform the lookup.
        // Should we create random peers to do the same?
        // Maybe it can relate to "resolve" function
        String pubKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        return Peer.valueOf(pubKey, "localhost", 32918);
    }

    /**
     * call back from PeerDialer
     * @param peer disconnected peer
     */
    @Override
    public void peerDisconnected(Peer peer) {
        tableMap.values().forEach(table -> table.dropPeer(peer));
    }
}
