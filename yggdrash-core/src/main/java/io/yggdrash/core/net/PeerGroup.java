/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerGroup implements BranchEventListener {

    private static final Logger log = LoggerFactory.getLogger(PeerGroup.class);

    private final Map<BranchId, PeerTable> peerTables = new ConcurrentHashMap<>();

    private final Map<BranchId, Map<PeerId, PeerClientChannel>> peerTableChannels
            = new ConcurrentHashMap<>();

    private final int maxPeers;

    private final Peer owner;

    private List<String> seedPeerList;

    public PeerGroup(Peer owner, int maxPeers) {
        this.owner = owner;
        this.maxPeers = maxPeers;
    }

    public Peer getOwner() {
        return owner;
    }

    public List<String> getBootstrappingSeedList(BranchId branchId) {
        List<String> seedPeerList;

        PeerTable table = getPeerTable(branchId);
        if (table == null || table.isPeerStoreEmpty()) {
            seedPeerList = getSeedPeerList();
        } else {
            seedPeerList = getPeerTable(branchId).getAllFromPeerStore();
        }

        if (seedPeerList == null || seedPeerList.isEmpty()) {
            return new ArrayList<>();
        }

        return seedPeerList;
    }

    public void addPeerTable(BranchId branchId, PeerStore peerStore) {
        if (peerTables.containsKey(branchId)) {
            throw new DuplicatedException(branchId.toString() + " branch duplicated");
        }
        PeerTable peerTable = new PeerTable(peerStore, owner);
        peerTables.put(branchId, peerTable);
    }

    void addPeerByYnodeUri(BranchId branchId, List<String> peerList) {
        for (String ynodeUri : peerList) {
            addPeerByYnodeUri(branchId, ynodeUri);
        }
    }

    public void addPeerByYnodeUri(BranchId branchId, String ynodeUri) {
        addPeer(branchId, Peer.valueOf(ynodeUri));
    }

    void addPeer(BranchId branchId, Peer peer) {

        if (peerTables.containsKey(branchId)) {
            getPeerTable(branchId).addPeer(peer);
        } else {
            log.info("Ignore branchId => {}", branchId);
        }
    }

    int count(BranchId branchId) {
        Optional<PeerTable> peerTable = Optional.ofNullable(getPeerTable(branchId));
        return peerTable.map(PeerTable::getPeersCount).orElse(0);
    }

    public List<String> getPeers(BranchId branchId, Peer peer) {
        if (!peerTables.containsKey(branchId)) {
            log.info("Ignore branchId => {}", branchId);
            return Collections.emptyList();
        }

        ArrayList<String> peerList = new ArrayList<>();

        PeerTable peerTable = peerTables.get(branchId);
        peerTable.getAllPeers().forEach(p -> peerList.add(p.toString()));
        peerTable.addPeer(peer);

        return peerList;
    }

    public List<Peer> getClosestPeers(BranchId branchId) {
        return Optional.ofNullable(getPeerTable(branchId))
                .map(o -> o.getClosestPeers(owner.getPeerId().getBytes()))
                .orElse(Collections.emptyList());
    }

    PeerTable getPeerTable(BranchId branchId) {
        return peerTables.getOrDefault(branchId, null);
    }

    boolean containsPeer(BranchId branchId, Peer peer) {
        if (peerTables.containsKey(branchId)) {
            return peerTables.get(branchId).contains(peer);
        } else {
            return false;
        }
    }

    boolean isPeerEmpty(BranchId branchId) {
        if (!peerTables.containsKey(branchId)) {
            return true;
        } else {
            return peerTables.get(branchId).getPeersCount() == 0;
        }
    }

    public boolean isChannelEmpty(BranchId branchId) {
        if (peerTableChannels.containsKey(branchId)) {
            return peerTableChannels.get(branchId).isEmpty();
        } else {
            return true;
        }
    }

    public List<String> getSeedPeerList() {
        return seedPeerList;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    List<String> getPeerUriList(BranchId branchId) {
        if (peerTables.containsKey(branchId)) {
            return peerTables.get(branchId).getAllPeers().stream()
                    .map(Peer::getYnodeUri).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public void destroy() {
        for (Map<PeerId, PeerClientChannel> peerChannel : peerTableChannels.values()) {
            peerChannel.values().forEach(PeerClientChannel::stop);
        }
    }

    public void healthCheck() {
        if (peerTableChannels.isEmpty()) {
            log.trace("Active peer is empty to health check peer");
            //throw new NonExistObjectException("Active peer is empty to health check peer");
            return;
        }
        log.debug("peerTableChannel" + peerTableChannels);

        for (Map.Entry<BranchId, Map<PeerId, PeerClientChannel>> entry
                : peerTableChannels.entrySet()) {
            BranchId branchId = entry.getKey();
            List<PeerClientChannel> peerChannelList
                    = new ArrayList<>(peerTableChannels.get(branchId).values());

            for (PeerClientChannel client : peerChannelList) {
                try {
                    Pong pong = client.ping("Ping");
                    if (pong.getPong().equals("Pong")) {
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Health check fail. peer=" + client.getPeer().getYnodeUri());
                }
                Peer peer = client.getPeer();
                peerTables.get(branchId).dropPeer(peer);
                peerTableChannels.get(branchId).remove(peer.getPeerId());

                client.stop();
            }
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        if (isChannelEmpty(tx.getBranchId())) {
            log.trace("Active peer is empty to broadcast transaction");
            return;
        }
        Proto.Transaction[] txns = new Proto.Transaction[] {tx.getInstance()};

        if (peerTableChannels.containsKey(tx.getBranchId())) {
            for (PeerClientChannel client : peerTableChannels.get(tx.getBranchId()).values()) {
                try {
                    client.broadcastTransaction(txns);
                } catch (Exception e) {
                    removePeerChannel(tx.getBranchId(), client);
                }
            }
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        if (isChannelEmpty(block.getBranchId())) {
            log.trace("Active peer is empty to broadcast block");
            return;
        }
        Proto.Block[] blocks = new Proto.Block[] {block.getInstance()};
        for (PeerClientChannel client : peerTableChannels.get(block.getBranchId()).values()) {
            try {
                client.broadcastBlock(blocks);
            } catch (Exception e) {
                removePeerChannel(block.getBranchId(), client);
            }
        }
    }

    private void removePeerChannel(BranchId branchId, PeerClientChannel client) {
        client.stop();
        peerTableChannels.get(branchId).remove(client.getPeer().getPeerId());
        peerTables.get(branchId).dropPeer(client.getPeer());
        log.debug("Removed channel size={}, peer size={}", peerTableChannels.get(branchId).size(),
                count(branchId));
    }

    public void newPeerChannel(BranchId branchId, PeerClientChannel client) {
        Peer peer = client.getPeer();

        Map<PeerId, PeerClientChannel> peerChannelList = peerTableChannels.get(branchId);
        if (peerChannelList == null) {
            peerChannelList = new ConcurrentHashMap<>();
            peerTableChannels.put(branchId, peerChannelList);
        }

        if (peerChannelList.containsKey(peer.getPeerId())) {
            return;
        } else if (peerChannelList.size() >= maxPeers) {
            log.info("Maximum number of peer channel exceeded. count={}, peer={}",
                    peerChannelList.size(), peer.getYnodeUri());
            return;
        }

        try {
            log.info("Connecting... peer {}:{}", peer.getHost(), peer.getPort());
            Pong pong = client.ping("Ping");
            // TODO validation peer
            if (pong.getPong().equals("Pong")) {
                // 접속 성공 시
                peerChannelList.put(peer.getPeerId(), client);
                log.info("Added size={}, channel={}",peerChannelList.size(), peer.toAddress());
            } else {
                // 접속 실패 시 목록 및 버킷에서 제거
                peerTables.get(branchId).dropPeer(peer);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer channel err=" + e.getMessage());
        }
    }

    public List<String> getActivePeerList() {
        List<String> activePeerList = new ArrayList<>();
        for (Map<PeerId, PeerClientChannel> peerTableChannel : peerTableChannels.values()) {
            List<String> branchChannelList = peerTableChannel.values().stream()
                    .map(channel -> channel.getPeer().toString())
                    .collect(Collectors.toList());
            activePeerList.addAll(branchChannelList);
        }
        return activePeerList;
    }

    public List<String> getActivePeerListOf(BranchId branchId) {
        return peerTableChannels.get(branchId)
                .values()
                .stream()
                .map(c -> String.format("%s:%d",c.getPeer().getHost(), c.getPeer().getPort()))
                .collect(Collectors.toList());
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<BlockHusk> syncBlock(BranchId branchId, long offset) {
        if (isChannelEmpty(branchId)) {
            log.trace("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        Map<PeerId, PeerClientChannel> peerClientChannelMap = peerTableChannels.get(branchId);
        PeerId key = (PeerId) peerClientChannelMap.keySet().toArray()[0];
        PeerClientChannel client = peerClientChannelMap.get(key);
        List<Proto.Block> blockList = client.syncBlock(branchId, offset);
        log.debug("Synchronize block offset={} receivedSize={}, from={}", offset, blockList.size(),
                client.getPeer());
        List<BlockHusk> syncList = new ArrayList<>(blockList.size());
        for (Proto.Block block : blockList) {
            syncList.add(new BlockHusk(block));
        }
        return syncList;
    }

    /**
     * Sync transaction list.
     *
     * @return the transaction list
     */
    public List<TransactionHusk> syncTransaction(BranchId branchId) {
        if (isChannelEmpty(branchId)) {
            log.trace("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        Map<PeerId, PeerClientChannel> peerClientChannelMap = peerTableChannels.get(branchId);
        PeerId key = (PeerId) peerClientChannelMap.keySet().toArray()[0];
        PeerClientChannel client = peerClientChannelMap.get(key);
        List<Proto.Transaction> txList = client.syncTransaction(branchId);
        log.info("Synchronize transaction receivedSize={}, from={}", txList.size(),
                client.getPeer());
        List<TransactionHusk> syncList = new ArrayList<>(txList.size());
        for (Proto.Transaction tx : txList) {
            syncList.add(new TransactionHusk(tx));
        }
        return syncList;
    }
}
