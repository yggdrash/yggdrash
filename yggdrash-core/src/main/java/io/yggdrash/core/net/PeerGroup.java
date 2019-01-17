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
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerGroup implements BranchEventListener {

    private static final Logger log = LoggerFactory.getLogger(PeerGroup.class);

    private final PeerTable table;

    private final Map<PeerId, PeerClientChannel> channelMap = new ConcurrentHashMap<>();

    private final int maxPeers;

    private final Peer owner;

    private List<String> seedPeerList;

    public PeerGroup(Peer owner, PeerStore peerStore, int maxPeers) {
        this.owner = owner;
        table = new PeerTable(peerStore, owner);
        this.maxPeers = maxPeers;
    }

    public Peer getOwner() {
        return owner;
    }

    List<String> getBootstrappingSeedList() {
        List<String> seedPeerList;

        if (table.isPeerStoreEmpty()) {
            seedPeerList = this.seedPeerList;
        } else {
            seedPeerList = table.getAllFromPeerStore();
        }

        if (seedPeerList == null || seedPeerList.isEmpty()) {
            return new ArrayList<>();
        }

        return seedPeerList;
    }

    void addPeerByYnodeUri(String ynodeUri) {
        table.addPeer(Peer.valueOf(ynodeUri));
    }

    int count() {
        return table.getPeersCount();
    }

    public List<String> getPeers(Peer peer) {
        // 현재 연결된 채널들의 버킷 아이디, 새로들어온 requestPeer 의 버킷아이디 로그
        log.debug("Received findPeers peer={}, bucketId={}",
                peer.toAddress(), logBucketIdOf(peer));
        logBucketIdOf();


        ArrayList<String> peerList = new ArrayList<>();

        table.getAllPeers().forEach(p -> peerList.add(p.toString()));
        table.addPeer(peer);
        return peerList;
    }

    List<Peer> getClosestPeers() {
        return table.getClosestPeers(owner.getPeerId().getBytes());
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    List<String> getPeerUriList() {
        return table.getAllPeers().stream()
                .map(Peer::getYnodeUri).collect(Collectors.toList());
    }

    public void destroy() {
        channelMap.values().forEach(PeerClientChannel::stop);
    }

    public void healthCheck() {
        if (channelMap.isEmpty()) {
            log.trace("Active peer is empty to health check peer");
            return;
        }

        for (PeerClientChannel client : channelMap.values()) {
            try {
                String pong = client.ping("Ping", owner);
                if ("Pong".equals(pong)) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Health check fail. peer=" + client.getPeer().getYnodeUri());
            }
            Peer peer = client.getPeer();
            table.dropPeer(peer);
            channelMap.remove(peer.getPeerId());

            client.stop();
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        if (channelMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast transaction");
            return;
        }
        Proto.Transaction[] txns = new Proto.Transaction[] {tx.getInstance()};

        for (PeerClientChannel client : channelMap.values()) {
            try {
                client.broadcastTransaction(txns);
            } catch (Exception e) {
                removePeerChannel(client);
            }
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        owner.updateBestBlock(BestBlock.of(block.getBranchId(), block.getIndex()));
        if (channelMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast block");
            return;
        }
        Proto.Block[] blocks = new Proto.Block[] {block.getInstance()};
        for (PeerClientChannel client : channelMap.values()) {
            try {
                client.broadcastBlock(blocks);
            } catch (Exception e) {
                removePeerChannel(client);
            }
        }
    }

    private void removePeerChannel(PeerClientChannel client) {
        client.stop();
        channelMap.remove(client.getPeer().getPeerId());
        table.dropPeer(client.getPeer());
        log.debug("Removed channel size={}, peer size={}",
                channelMap.size(), table.getPeersCount());
    }

    public boolean isMaxChannel() {
        return channelMap.size() >= maxPeers;
    }

    public void addChannel(PeerClientChannel client) {
        Peer peer = client.getPeer();

        if (channelMap.containsKey(peer.getPeerId())) {
            return;
        }

        try {
            log.info("Connecting... peer {}:{}", peer.getHost(), peer.getPort());
            String pong = client.ping("Ping", owner);
            // TODO validation peer
            if ("Pong".equals(pong)) {
                // 접속 성공 시
                if (!isMaxChannel()) {
                    channelMap.put(peer.getPeerId(), client);
                    log.info("Added size={}, channel={}", channelMap.size(), peer.toAddress());
                }
            } else {
                // 접속 실패 시 목록 및 버킷에서 제거
                table.dropPeer(peer);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer channel err=" + e.getMessage());
        }
    }

    public boolean isClosePeer(Peer requestPeer) {
        List<Peer> peerList = getClosestPeers();
        if (peerList.size() < maxPeers) {
            return peerList.contains(requestPeer);
        }
        return peerList.subList(0, maxPeers).contains(requestPeer);
    }

    void reloadPeerChannel(PeerClientChannel client) {
        log.info("reloadPeerChannel : peer = {}", client.getPeer());

        PeerId requestPeerId = client.getPeer().getPeerId();
        if (channelMap.containsKey(requestPeerId)) {
            channelMap.put(requestPeerId, client);
        } else {
            List<Peer> peerList = getClosestPeers();
            if (peerList.size() > maxPeers) {
                peerList = peerList.subList(0, maxPeers);
            }
            List<PeerId> newClosestPeers = peerList.stream()
                    .map(Peer::getPeerId).collect(Collectors.toList());
            newClosestPeers.remove(requestPeerId);

            for (PeerId key : channelMap.keySet()) {
                if (!newClosestPeers.contains(key)) {
                    channelMap.remove(key);
                }
            }

            if (newClosestPeers.size() >= maxPeers - 1) {
                PeerId lastPeerId = newClosestPeers.get(maxPeers - 1);
                channelMap.remove(lastPeerId);
            } else {
                log.warn("newClosestPeers size=" + newClosestPeers.size());
            }

        }
    }

    public List<Peer> getLatestPeers(long reqTime) {
        return table.getLatestPeers(reqTime);
    }

    private int logBucketIdOf(Peer peer) {
        return table.getTmpBucketId(peer);
    }

    private void logBucketIdOf() {
        channelMap.values().forEach(
                peerClientChannel
                        -> log.debug("Current peerClientChannel => peer={}:{}, bucketId={}",
                        peerClientChannel.getPeer().getHost(),
                        peerClientChannel.getPeer().getPort(),
                        table.getTmpBucketId(peerClientChannel.getPeer())));
    }

    public Map<Integer, List<Peer>> getBucketsOf() {
        return table.getBucketIdAndPeerList();
    }

    public List<String> getAllPeersFromBucketsOf() {
        return table.getAllPeers()
                .stream()
                .map(p -> String.format("%s:%d", p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }

    public List<String> getActivePeerList() {
        return channelMap.values().stream()
                .map(channel -> channel.getPeer().toString())
                .collect(Collectors.toList());
    }

    public List<String> getActivePeerListOf() {
        return channelMap
                .values()
                .stream()
                .map(c -> String.format("%s:%d", c.getPeer().getHost(), c.getPeer().getPort()))
                .collect(Collectors.toList());
    }

    public void touchPeer(Peer peer) {
        if (table.contains(peer)) {
            table.touchPeer(peer);
        }
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<BlockHusk> syncBlock(BranchId branchId, long offset) {
        if (channelMap.isEmpty()) {
            log.trace("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        PeerId key = (PeerId) channelMap.keySet().toArray()[0];
        PeerClientChannel client = channelMap.get(key);
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
        if (channelMap.isEmpty()) {
            log.trace("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        PeerId key = (PeerId) channelMap.keySet().toArray()[0];
        PeerClientChannel client = channelMap.get(key);
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
