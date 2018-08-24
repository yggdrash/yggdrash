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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.GenesisFrontierParam;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Runtime;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.net.GrpcClientChannel;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class NodeManagerImpl implements NodeManager {
    private static final Logger log = LoggerFactory.getLogger(NodeManager.class);

    private BlockChain blockChain;

    private TransactionStore transactionStore;

    private NodeProperties nodeProperties;

    private Wallet wallet;

    private PeerGroup peerGroup;

    private Peer peer;

    private MessageSender<PeerClientChannel> messageSender;

    private NodeHealthIndicator nodeHealthIndicator;

    private Runtime runtime;

    @Autowired
    public void setNodeProperties(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Autowired
    public void setTransactionStore(TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
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

    @Autowired
    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy uri=" + peer.getYnodeUri());
        messageSender.destroy(peer.getYnodeUri());
    }

    @Override
    public void init() {
        try {
            //transactionStore.putDummyTx("4");
            initFrontiers();
            Set<TransactionHusk> txList = transactionStore.getAll();
            executeAllTx(txList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FailedOperationException(e);
        }

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

    private void executeAllTx(Set<TransactionHusk> txList) {
        CoinContract coinContract = new CoinContract();
        try {
            for (TransactionHusk tx : txList) {
                if (!runtime.invoke(coinContract,tx)) {
                    break;
                }

            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    @Override
    public TransactionHusk getTxByHash(String id) {
        return getTxByHash(new Sha3Hash(id));
    }

    @Override
    public TransactionHusk getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    @Override
    public TransactionHusk addTransaction(TransactionHusk tx) {
        if (transactionStore.contains(tx.getHash())) {
            throw new FailedOperationException("Duplicated " + tx.getHash().toString()
                    + " Transaction");
        } else if (!tx.verify()) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            messageSender.newTransaction(tx);
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException("Transaction");
        }
    }

    @Override
    public List<TransactionHusk> getTransactionList() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    @Override
    public Set<BlockHusk> getBlocks() {
        return blockChain.getBlocks();
    }

    @Override
    public BlockHusk generateBlock() {
        BlockHusk block = BlockHusk.build(wallet,
                new ArrayList<>(transactionStore.getUnconfirmedTxs()), blockChain.getPrevBlock());
        blockChain.addBlock(block);
        executeAllTx(new TreeSet<>(block.getBody()));
        messageSender.newBlock(block);
        removeTxByBlock(block);
        return block;
    }

    @Override
    public BlockHusk addBlock(BlockHusk block) {
        BlockHusk newBlock = null;
        if (blockChain.isGenesisBlockChain() && block.getIndex() == 0) {
            blockChain.addBlock(block);
            newBlock = block;
        } else if (blockChain.getPrevBlock().nextIndex() == block.getIndex()) {
            blockChain.addBlock(block);
            newBlock = block;
        }
        removeTxByBlock(block);
        return newBlock;
    }

    @Override
    public BlockHusk getBlockByIndexOrHash(String indexOrHash) {

        if (isNumeric(indexOrHash)) {
            int index = Integer.parseInt(indexOrHash);
            return blockChain.getBlockByIndex(index);
        } else {
            return blockChain.getBlockByHash(indexOrHash);
        }
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
            List<BlockHusk> blockList = messageSender.syncBlock(blockChain.getLastIndex());
            for (BlockHusk block : blockList) {
                blockChain.addBlock(block);
            }
            List<TransactionHusk> txList = messageSender.syncTransaction();
            for (TransactionHusk tx : txList) {
                transactionStore.put(tx.getHash(), tx);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void removeTxByBlock(BlockHusk block) {
        if (block == null || block.getBody() == null) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (TransactionHusk tx : block.getBody()) {
            keys.add(tx.getHash());
        }
        this.transactionStore.batch(keys);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
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
    public void setBlockChain(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    private void initFrontiers() throws Exception {
        if (blockChain.getLastIndex() > 1) {
            log.warn("It's not a genesis blockchain");
            return;
        }
        // TODO temporary execute genesis yeed tx
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Proto.Transaction tx = blockChain.getBlockByIndex(0).getInstance().getBodyList().get(0);
        GenesisFrontierParam param = mapper.readValue(tx.getBody(), GenesisFrontierParam.class);
        if (!param.isGenesisOp()) {
            return;
        }
        for (Map.Entry<String, GenesisFrontierParam.Balance> element : param.getFrontier()
                .entrySet()) {
            String balance = element.getValue().getBalance();
            runtime.getStateStore().getState().put(element.getKey(), Long.parseLong(balance));
        }
    }
}
