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

import com.google.common.annotations.VisibleForTesting;
import io.yggdrash.core.Block;
import io.yggdrash.core.NodeEventListener;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.mapper.BlockMapper;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.core.net.NodeSyncClient;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageSender implements DisposableBean, NodeEventListener {
    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private final PeerGroup peerGroup;

    private List<String> seedPeerList;

    private List<NodeSyncClient> activePeerList = Collections.synchronizedList(new ArrayList<>());

    public MessageSender(PeerGroup peerGroup, NodeProperties nodeProperties) {
        this.peerGroup = peerGroup;
        this.seedPeerList = nodeProperties.getSeedPeerList();
    }

    @PostConstruct
    @VisibleForTesting
    public void init() {
        if (seedPeerList == null || seedPeerList.isEmpty()) {
            return;
        }
        for (String ynode : seedPeerList) {
            try {
                Peer peer = Peer.valueOf(ynode);
                log.info("Trying to connecting SEED peer at {}", ynode);
                NodeSyncClient client = new NodeSyncClient(peer.getHost(), peer.getPort());
                Pong pong = client.ping("Ping");
                // TODO validation peer(encrypting msg by privateKey and signing by publicKey ...)
                if (!pong.getPong().equals("Pong")) {
                    continue;
                }
                addPeer(client.getPeerList());
            } catch(Exception e) {
                log.warn("ynode={}, error={}", ynode, e.getMessage());
            }
        }
        addActivePeer();
    }

    @PreDestroy
    public void destroy() {
        for (NodeSyncClient client : activePeerList) {
            client.stop();
        }
    }

    private void addPeer(List<String> peerList) {
        for (String ynode : peerList) {
            try {
                Peer peer = Peer.valueOf(ynode);
                peerGroup.addPeer(peer);
            } catch(Exception e) {
                log.warn("ynode={}, error={}", ynode, e.getMessage());
            }
        }
    }

    private void addActivePeer() {
        for (Peer peer : peerGroup.getPeers()) {
            log.info("Trying to connecting peer at {}:{}", peer.getHost(), peer.getPort());
            NodeSyncClient client = new NodeSyncClient(peer.getHost(), peer.getPort());
            Pong pong = client.ping("Ping");
            // TODO validation peer
            if (!pong.getPong().equals("Pong")) {
                continue;
            }
            activePeerList.add(client);
        }
    }

    public List<String> getPeerIdList() {
        return peerGroup.getPeers().stream().map(Peer::getIdShort).collect(Collectors.toList());
    }

    public void ping() {
        for (NodeSyncClient client : activePeerList) {
            client.ping("Ping");
        }
    }

    @Override
    public void newTransaction(Transaction tx) {
        BlockChainProto.Transaction protoTx
                = TransactionMapper.transactionToProtoTransaction(tx);
        BlockChainProto.Transaction[] txns = new BlockChainProto.Transaction[] {protoTx};

        for (NodeSyncClient client : activePeerList) {
            client.broadcastTransaction(txns);
        }
    }

    @Override
    public void newBlock(Block block) {
        BlockChainProto.Block[] blocks
                = new BlockChainProto.Block[] {BlockMapper.blockToProtoBlock(block)};
        for (NodeSyncClient client : activePeerList) {
            client.broadcastBlock(blocks);
        }
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    @Override
    public List<Block> syncBlock(long offset) throws IOException {
        if (activePeerList.isEmpty()) {
            log.warn("Active peer is empty.");
        }
        // TODO sync peer selection policy
        List<BlockChainProto.Block> blockList = activePeerList.get(0).syncBlock(offset);
        log.debug("Synchronize block offset=" + offset);
        if (blockList == null || blockList.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("Synchronize block received=" + blockList.size());
        List<Block> syncList = new ArrayList<>(blockList.size());
        for (BlockChainProto.Block block : blockList) {
            syncList.add(BlockMapper.protoBlockToBlock(block));
        }
        return syncList;
    }

    @Override
    public List<Transaction> syncTransaction() throws IOException {
        // TODO sync peer selection policy
        List<BlockChainProto.Transaction> txList = activePeerList.get(0).syncTransaction();
        log.debug("Synchronize transaction received=" + txList.size());
        List<Transaction> syncList = new ArrayList<>(txList.size());
        for (BlockChainProto.Transaction tx : txList) {
            syncList.add(TransactionMapper.protoTransactionToTransaction(tx));
        }
        return syncList;
    }
}
