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

package io.yggdrash.node;

import io.yggdrash.core.Block;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.event.PeerEventListener;
import io.yggdrash.core.mapper.BlockMapper;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.core.net.NodeSyncClient;
import io.yggdrash.core.net.Peer;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageSender {
    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private final Map<String, NodeSyncClient> peerChannel = new ConcurrentHashMap<>();

    private PeerEventListener listener;

    public void setListener(PeerEventListener listener) {
        this.listener = listener;
    }

    public void destroy(String ynodeUri) {
        peerChannel.values().forEach(client -> client.stop(ynodeUri));
    }

    void ping() {
        List<NodeSyncClient> peerChannelList = new ArrayList<>(peerChannel.values());
        for (NodeSyncClient client : peerChannelList) {
            try {
                Pong pong = client.ping("Ping");
                if (pong.getPong().equals("Pong")) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Health check fail. peer=" + client.getPeer().getYnodeUri());
            }
            peerChannel.remove(client.getPeer().getYnodeUri());
            client.stop();
            listener.disconnected(client.getPeer());
        }
    }

    public void newTransaction(Transaction tx) {
        BlockChainProto.Transaction protoTx
                = TransactionMapper.transactionToProtoTransaction(tx);
        BlockChainProto.Transaction[] txns = new BlockChainProto.Transaction[] {protoTx};

        for (NodeSyncClient client : peerChannel.values()) {
            client.broadcastTransaction(txns);
        }
    }

    public void newBlock(Block block) {
        BlockChainProto.Block[] blocks
                = new BlockChainProto.Block[] {BlockMapper.blockToProtoBlock(block)};
        for (NodeSyncClient client : peerChannel.values()) {
            client.broadcastBlock(blocks);
        }
    }

    public void newPeerChannel(Peer peer) {
        if (peerChannel.containsKey(peer.getYnodeUri())) {
            return;
        }
        try {
            NodeSyncClient client = new NodeSyncClient(peer);
            log.info("Connecting... peer {}:{}", peer.getHost(), peer.getPort());
            Pong pong = client.ping("Ping");
            // TODO validation peer
            if (pong.getPong().equals("Pong")) {
                peerChannel.put(peer.getYnodeUri(), client);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the activePeerList err=" + e.getMessage());
        }
    }

    public List<String> getActivePeerList() {
        return new ArrayList<>(peerChannel.keySet());
    }

    /**
     * Broadcast peer uri
     *
     * @param ynodeUri the peer uri to broadcast
     * @return the block list
     */
    public List<String> broadcastPeerConnect(String ynodeUri) {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to broadcast peer");
            return Collections.emptyList();
        }
        List<String> peerList = new ArrayList<>();
        for (NodeSyncClient client : peerChannel.values()) {
            peerList.addAll(client.requestPeerList(ynodeUri, 0));
        }
        return peerList;
    }

    public void broadcastPeerDisconnect(String ynodeUri) {
        NodeSyncClient disconnectedPeer = peerChannel.remove(ynodeUri);
        if (disconnectedPeer != null) {
            disconnectedPeer.stop();
        }
        for (NodeSyncClient client : peerChannel.values()) {
            client.disconnectPeer(ynodeUri);
        }
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<Block> syncBlock(long offset) {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannel.keySet().toArray()[0];
        NodeSyncClient client = peerChannel.get(key);
        List<BlockChainProto.Block> blockList = client.syncBlock(offset);
        log.debug("Synchronize block received=" + blockList.size());
        List<Block> syncList = new ArrayList<>(blockList.size());
        for (BlockChainProto.Block block : blockList) {
            syncList.add(BlockMapper.protoBlockToBlock(block));
        }
        return syncList;
    }

    /**
     * Sync transaction list.
     *
     * @return the transaction list
     */
    public List<Transaction> syncTransaction() {
        if (peerChannel.isEmpty()) {
            log.warn("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) peerChannel.keySet().toArray()[0];
        NodeSyncClient client = peerChannel.get(key);
        List<BlockChainProto.Transaction> txList = client.syncTransaction();
        log.debug("Synchronize transaction received=" + txList.size());
        List<Transaction> syncList = new ArrayList<>(txList.size());
        for (BlockChainProto.Transaction tx : txList) {
            syncList.add(TransactionMapper.protoTransactionToTransaction(tx));
        }
        return syncList;
    }
}
