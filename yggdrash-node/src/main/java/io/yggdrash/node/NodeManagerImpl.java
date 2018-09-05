/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.net.GrpcClientChannel;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.config.NodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NodeManagerImpl implements NodeManager {
    private static final Logger log = LoggerFactory.getLogger(NodeManager.class);

    private BranchGroup branchGroup;

    private NodeProperties nodeProperties;

    private Wallet wallet;

    private PeerGroup peerGroup;

    private Peer peer;

    private MessageSender<PeerClientChannel> messageSender;

    private NodeHealthIndicator nodeHealthIndicator;

    @Autowired
    public void setNodeProperties(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Autowired
    public void setPeerGroup(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Autowired
    public void setMessageSender(MessageSender<PeerClientChannel> messageSender) {
        this.messageSender = messageSender;
    }

    @Autowired
    public void setNodeHealthIndicator(NodeHealthIndicator nodeHealthIndicator) {
        this.nodeHealthIndicator = nodeHealthIndicator;
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy uri=" + peer.getYnodeUri());
        messageSender.destroy(peer.getYnodeUri());
    }

    @Override
    public void init() {
        messageSender.setListener(this);
        NodeProperties.Grpc grpc = nodeProperties.getGrpc();
        peer = Peer.valueOf(wallet.getNodeId(), grpc.getHost(), grpc.getPort());
        requestPeerList();
        activatePeers();
        if (!peerGroup.isEmpty()) {
            nodeHealthIndicator.sync();
            syncBlockAndTransaction();
        }
        peerGroup.addPeer(peer);
        log.info("Init node=" + peer.getYnodeUri());
        nodeHealthIndicator.up();
    }

    @Override
    public TransactionHusk getTxByHash(String id) {
        return getTxByHash(new Sha3Hash(id));
    }

    @Override
    public TransactionHusk getTxByHash(Sha3Hash hash) {
        return branchGroup.getTxByHash(hash);
    }

    @Override
    public TransactionHusk addTransaction(TransactionHusk tx) {
        branchGroup.addTransaction(tx);
        messageSender.newTransaction(tx);
        return tx;
    }

    @Override
    public List<TransactionHusk> getTransactionList() {
        return branchGroup.getTransactionList();
    }

    @Override
    public Set<BlockHusk> getBlocks() {
        return branchGroup.getBlocks();
    }

    @Override
    public BlockHusk generateBlock() {
        BlockHusk block = branchGroup.generateBlock(wallet);
        messageSender.newBlock(block);
        return block;
    }

    @Override
    public BlockHusk addBlock(BlockHusk block) {
        return branchGroup.addBlock(block);
    }

    @Override
    public BlockHusk getBlockByIndexOrHash(String indexOrHash) {
        return branchGroup.getBlockByIndexOrHash(indexOrHash);
    }

    @Override
    public String getNodeUri() {
        return peer.getYnodeUri();
    }

    @Override
    public void addPeer(String ynodeUri) {
        if (peerGroup.contains(ynodeUri)) {
            log.debug("Yggdrash node is exist. uri={}", ynodeUri);
            return;
        }
        Peer peer = addPeerByYnodeUri(ynodeUri);
        List<String> peerList = messageSender.broadcastPeerConnect(ynodeUri);
        addPeerByYnodeUri(peerList);
        addActivePeer(peer);
    }

    @Override
    public void removePeer(String ynodeUri) {
        if (peerGroup.removePeer(ynodeUri) != null) {
            messageSender.broadcastPeerDisconnect(ynodeUri);
        }
    }

    @Override
    public List<String> getPeerUriList() {
        return peerGroup.getPeers().stream().map(Peer::getYnodeUri).collect(Collectors.toList());
    }

    private void addPeerByYnodeUri(List<String> peerList) {
        for (String ynodeUri : peerList) {
            addPeerByYnodeUri(ynodeUri);
        }
    }

    private Peer addPeerByYnodeUri(String ynodeUri) {
        try {
            if (peerGroup.count() >= nodeProperties.getMaxPeers()) {
                log.warn("Ignore to add the peer. count={}, peer={}", peerGroup.count(), ynodeUri);
                return null;
            }
            Peer peer = Peer.valueOf(ynodeUri);
            return peerGroup.addPeer(peer);
        } catch (Exception e) {
            log.warn("ynode={}, error={}", ynodeUri, e.getMessage());
        }
        return null;
    }

    private void activatePeers() {
        for (Peer peer : peerGroup.getPeers()) {
            addActivePeer(peer);
        }
    }

    private void addActivePeer(Peer peer) {
        if (peer == null || this.peer.getYnodeUri().equals(peer.getYnodeUri())) {
            return;
        }
        messageSender.newPeerChannel(new GrpcClientChannel(peer));
    }

    private void requestPeerList() {
        List<String> seedPeerList = peerGroup.getSeedPeerList();
        if (seedPeerList == null || seedPeerList.isEmpty()) {
            return;
        }
        for (String ynodeUri : seedPeerList) {
            if (ynodeUri.equals(peer.getYnodeUri())) {
                continue;
            }
            try {
                Peer peer = Peer.valueOf(ynodeUri);
                log.info("Trying to connecting SEED peer at {}", ynodeUri);
                GrpcClientChannel client = new GrpcClientChannel(peer);
                // TODO validation peer(encrypting msg by privateKey and signing by publicKey ...)
                List<String> peerList = client.requestPeerList(getNodeUri(), 0);
                client.stop();
                addPeerByYnodeUri(peerList);
            } catch (Exception e) {
                log.warn("ynode={}, error={}", ynodeUri, e.getMessage());
            }
        }
    }

    private void syncBlockAndTransaction() {
        try {
            List<BlockHusk> blockList = messageSender.syncBlock(branchGroup.getLastIndex());
            for (BlockHusk block : blockList) {
                branchGroup.addBlock(block);
            }
            List<TransactionHusk> txList = messageSender.syncTransaction();
            for (TransactionHusk tx : txList) {
                branchGroup.addTransaction(tx);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public Wallet getWallet() {
        return wallet;
    }

    @Autowired
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public void disconnected(Peer peer) {
        removePeer(peer.getYnodeUri());
    }

    @Autowired
    public void setBranchGroup(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }
}
