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

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.event.BranchEventListener;
import io.yggdrash.core.event.PeerEventListener;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerGroup implements BranchEventListener {

    private static final Logger log = LoggerFactory.getLogger(PeerGroup.class);

    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    private final Map<String, PeerClientChannel> peerChannels = new ConcurrentHashMap<>();

    private final int maxPeers;

    private List<String> seedPeerList;

    private PeerEventListener listener;

    public PeerGroup(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    public void setListener(PeerEventListener listener) {
        this.listener = listener;
    }

    public void addPeerByYnodeUri(List<String> peerList) {
        for (String ynodeUri : peerList) {
            Peer peer = Peer.valueOf(ynodeUri);
            addPeer(peer);
        }
    }

    public void addPeer(String ynodeUri) {
        addPeer(Peer.valueOf(ynodeUri));
    }

    public void addPeer(Peer peer) {
        String ynodeUri = peer.getYnodeUri();
        if (peers.containsKey(ynodeUri)) {
            log.debug("Yggdrash node is exist. uri={}", ynodeUri);
            return;
        } else if (peers.size() >= maxPeers) {
            log.warn("Ignore to add the peer. count={}, peer={}", peers.size(), ynodeUri);
            return;
        }
        peers.put(ynodeUri, peer);
        if (listener != null) {
            listener.newPeer(peer);
        }
        broadcastPeerConnect(ynodeUri);
    }

    public int count() {
        return peers.size();
    }

    public void disconnected(String ynodeUri) {
        Peer removed = peers.remove(ynodeUri);
        if (removed != null) {
            broadcastPeerDisconnect(ynodeUri);
        }
    }

    public Collection<Peer> getPeers() {
        return peers.values();
    }

    public boolean contains(String ynodeUri) {
        return peers.containsKey(ynodeUri);
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }

    public void clear() {
        this.peers.clear();
    }

    public List<String> getSeedPeerList() {
        return seedPeerList;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    public List<String> getPeerUriList() {
        return peers.values().stream().map(Peer::getYnodeUri).collect(Collectors.toList());
    }

    public void destroy(String ynodeUri) {
        peerChannels.values().forEach(client -> client.stop(ynodeUri));
    }

    public void healthCheck() {
        if (peerChannels.isEmpty()) {
            log.trace("Active peer is empty to health check peer");
            return;
        }
        List<PeerClientChannel> peerChannelList = new ArrayList<>(peerChannels.values());
        for (PeerClientChannel client : peerChannelList) {
            try {
                Pong pong = client.ping("Ping");
                if (pong.getPong().equals("Pong")) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Health check fail. peer=" + client.getPeer().getYnodeUri());
            }
            String ynodeUri = client.getPeer().getYnodeUri();
            peerChannels.remove(ynodeUri);
            client.stop();
            disconnected(ynodeUri);
        }
    }

    public void newTransaction(TransactionHusk tx) {
        if (peerChannels.isEmpty()) {
            log.warn("Active peer is empty to broadcast transaction");
        }
        Proto.Transaction[] txns = new Proto.Transaction[] {tx.getInstance()};

        for (PeerClientChannel client : peerChannels.values()) {
            client.broadcastTransaction(txns);
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        if (peerChannels.isEmpty()) {
            log.trace("Active peer is empty to broadcast block");
        }
        Proto.Block[] blocks
                = new Proto.Block[] {block.getInstance()};
        for (PeerClientChannel client : peerChannels.values()) {
            client.broadcastBlock(blocks);
        }
    }

    public void newPeerChannel(PeerClientChannel client) {
        Peer peer = client.getPeer();
        if (peerChannels.containsKey(peer.getYnodeUri())) {
            return;
        } else if (peerChannels.size() >= maxPeers) {
            log.info("Ignore to add active peer channel. count={}, peer={}", peerChannels.size(),
                    peer.getYnodeUri());
            return;
        }
        try {
            log.info("Connecting... peer {}:{}", peer.getHost(), peer.getPort());
            Pong pong = client.ping("Ping");
            // TODO validation peer
            if (pong.getPong().equals("Pong")) {
                peerChannels.put(peer.getYnodeUri(), client);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer channel err=" + e.getMessage());
        }
    }

    public List<String> getActivePeerList() {
        return new ArrayList<>(peerChannels.keySet());
    }

    /**
     * Broadcast peer uri
     *
     * @param ynodeUri the peer uri to broadcast
     * @return the block list
     */
    public List<String> broadcastPeerConnect(String ynodeUri) {
        if (peerChannels.isEmpty()) {
            log.warn("Active peer is empty to broadcast peer");
            return Collections.emptyList();
        }
        List<String> peerList = new ArrayList<>();
        for (PeerClientChannel client : peerChannels.values()) {
            peerList.addAll(client.requestPeerList(ynodeUri, 0));
        }
        return peerList;
    }

    public void broadcastPeerDisconnect(String ynodeUri) {
        PeerClientChannel disconnectedPeer = peerChannels.remove(ynodeUri);
        if (disconnectedPeer != null) {
            disconnectedPeer.stop();
        }
        for (PeerClientChannel client : peerChannels.values()) {
            client.disconnectPeer(ynodeUri);
        }
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<BlockHusk> syncBlock(long offset) {
        if (peerChannels.isEmpty()) {
            log.warn("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannels.keySet().toArray()[0];
        PeerClientChannel client = peerChannels.get(key);
        List<Proto.Block> blockList = client.syncBlock(offset);
        log.debug("Synchronize block received=" + blockList.size());
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
    public List<TransactionHusk> syncTransaction() {
        if (peerChannels.isEmpty()) {
            log.warn("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannels.keySet().toArray()[0];
        PeerClientChannel client = peerChannels.get(key);
        List<Proto.Transaction> txList = client.syncTransaction();
        log.debug("Synchronize transaction received=" + txList.size());
        List<TransactionHusk> syncList = new ArrayList<>(txList.size());
        for (Proto.Transaction tx : txList) {
            syncList.add(new TransactionHusk(tx));
        }
        return syncList;
    }
}
