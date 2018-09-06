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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.net.NodeManager;
import io.yggdrash.core.net.NodeServer;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerChannelGroup;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GRpcNodeServer implements NodeServer, NodeManager {
    private static final Logger log = LoggerFactory.getLogger(NodeManager.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private BranchGroup branchGroup;

    private PeerGroup peerGroup;

    private PeerChannelGroup peerChannelGroup;

    private Wallet wallet;

    private Peer peer;

    private NodeHealthIndicator nodeHealthIndicator;

    private Server server;

    private int maxPeers;

    @Autowired
    public void setPeerGroup(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Autowired
    public void setPeerChannelGroup(PeerChannelGroup peerChannelGroup) {
        this.peerChannelGroup = peerChannelGroup;
        peerChannelGroup.setListener(this);
    }

    @Autowired
    public void setNodeHealthIndicator(NodeHealthIndicator nodeHealthIndicator) {
        this.nodeHealthIndicator = nodeHealthIndicator;
    }

    public Wallet getWallet() {
        return wallet;
    }

    @Autowired
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    @Autowired
    public void setBranchGroup(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
        branchGroup.setListener(peerChannelGroup);
    }

    @Override
    public void start(String host, int port) throws IOException {
        this.peer = Peer.valueOf(wallet.getNodeId(), host, port);
        this.server = ServerBuilder.forPort(port)
                .addService(new PingPongImpl())
                .addService(new BlockChainImpl(this, peerGroup, branchGroup))
                .build()
                .start();
        log.info("GRPC Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            this.stop();
            System.err.println("*** server shut down");
        }));
        init();
    }

    @Override
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy uri=" + peer.getYnodeUri());
        peerChannelGroup.destroy(peer.getYnodeUri());
    }

    private void init() {
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
    public void generateBlock() {
        branchGroup.generateBlock(wallet);
    }

    @Override
    public String getNodeUri() {
        return peer.getYnodeUri();
    }

    @Override
    public void setMaxPeers(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    @Override
    public void addPeer(String ynodeUri) {
        if (peerGroup.contains(ynodeUri)) {
            log.debug("Yggdrash node is exist. uri={}", ynodeUri);
            return;
        }
        Peer peer = addPeerByYnodeUri(ynodeUri);
        List<String> peerList = peerChannelGroup.broadcastPeerConnect(ynodeUri);
        addPeerByYnodeUri(peerList);
        addActivePeer(peer);
    }

    @Override
    public void disconnected(Peer peer) {
        if (peerGroup.removePeer(peer.getYnodeUri()) != null) {
            peerChannelGroup.broadcastPeerDisconnect(peer.getYnodeUri());
        }
    }

    private void addPeerByYnodeUri(List<String> peerList) {
        for (String ynodeUri : peerList) {
            addPeerByYnodeUri(ynodeUri);
        }
    }

    private Peer addPeerByYnodeUri(String ynodeUri) {
        try {
            if (peerGroup.count() >= maxPeers) {
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
        peerChannelGroup.newPeerChannel(new GRpcClientChannel(peer));
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
                GRpcClientChannel client = new GRpcClientChannel(peer);
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
            List<BlockHusk> blockList = peerChannelGroup.syncBlock(branchGroup.getLastIndex());
            for (BlockHusk block : blockList) {
                branchGroup.addBlock(block);
            }
            List<TransactionHusk> txList = peerChannelGroup.syncTransaction();
            for (TransactionHusk tx : txList) {
                branchGroup.addTransaction(tx);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    static class BlockChainImpl extends BlockChainGrpc.BlockChainImplBase {
        private NodeManager nodeManager;
        private PeerGroup peerGroup;
        private BranchGroup branchGroup;

        BlockChainImpl(NodeManager nodeManager, PeerGroup peerGroup, BranchGroup branchGroup) {
            this.nodeManager = nodeManager;
            this.peerGroup = peerGroup;
            this.branchGroup = branchGroup;
        }

        private static final Set<StreamObserver<NetProto.Empty>> txObservers =
                ConcurrentHashMap.newKeySet();
        private static final Set<StreamObserver<NetProto.Empty>> blockObservers =
                ConcurrentHashMap.newKeySet();

        /**
         * Sync block response
         *
         * @param syncLimit        the start block index and limit to sync
         * @param responseObserver the observer response to the block list
         */
        @Override
        public void syncBlock(NetProto.SyncLimit syncLimit,
                              StreamObserver<Proto.BlockList> responseObserver) {
            long offset = syncLimit.getOffset();
            long limit = syncLimit.getLimit();
            log.debug("Synchronize block request offset={}, limit={}", offset, limit);

            Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
            for (BlockHusk husk : branchGroup.getBlocks()) {
                if (husk.getIndex() >= offset) {
                    builder.addBlocks(husk.getInstance());
                }
                if (limit > 0 && builder.getBlocksCount() > limit) {
                    break;
                }
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        /**
         * Sync transaction response
         *
         * @param empty            the empty message
         * @param responseObserver the observer response to the transaction list
         */
        @Override
        public void syncTransaction(NetProto.Empty empty,
                                    StreamObserver<Proto.TransactionList> responseObserver) {
            log.debug("Synchronize tx request");
            Proto.TransactionList.Builder builder
                    = Proto.TransactionList.newBuilder();
            for (TransactionHusk husk : branchGroup.getTransactionList()) {
                builder.addTransactions(husk.getInstance());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        /**
         * Peer list response
         *
         * @param peerRequest      the request with limit of peer and peer uri
         * @param responseObserver the observer response to the peer list
         */
        @Override
        public void requestPeerList(NetProto.PeerRequest peerRequest,
                                    StreamObserver<NetProto.PeerList> responseObserver) {
            log.debug("Synchronize peer request from=" + peerRequest.getFrom());
            NetProto.PeerList.Builder builder = NetProto.PeerList.newBuilder();

            List<String> peerUriList = peerGroup.getPeerUriList();

            if (peerRequest.getLimit() > 0) {
                int limit = peerRequest.getLimit();
                builder.addAllPeers(peerUriList.stream().limit(limit).collect(Collectors.toList()));
            } else {
                builder.addAllPeers(peerUriList);
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            nodeManager.addPeer(peerRequest.getFrom());
        }

        /**
         * Broadcast a disconnected peer
         *
         * @param peerRequest      the request with disconnected peer uri
         * @param responseObserver the empty response
         */
        @Override
        public void disconnectPeer(NetProto.PeerRequest peerRequest,
                                   StreamObserver<NetProto.Empty> responseObserver) {
            log.debug("Received disconnect for=" + peerRequest.getFrom());
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            nodeManager.disconnected(Peer.valueOf(peerRequest.getFrom()));
        }

        @Override
        public StreamObserver<Proto.Transaction> broadcastTransaction(
                StreamObserver<NetProto.Empty> responseObserver) {

            txObservers.add(responseObserver);

            return new StreamObserver<Proto.Transaction>() {
                @Override
                public void onNext(Proto.Transaction protoTx) {
                    log.debug("Received transaction: {}", protoTx);
                    TransactionHusk tx = new TransactionHusk(protoTx);
                    TransactionHusk newTx = branchGroup.addTransaction(tx);
                    // ignore broadcast by other node's broadcast
                    if (newTx == null) {
                        return;
                    }

                    for (StreamObserver<NetProto.Empty> observer : txObservers) {
                        observer.onNext(EMPTY);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Broadcasting transaction failed: {}", t.getMessage());
                    txObservers.remove(responseObserver);
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    txObservers.remove(responseObserver);
                    responseObserver.onCompleted();
                }
            };
        }

        @Override
        public StreamObserver<Proto.Block> broadcastBlock(
                StreamObserver<NetProto.Empty> responseObserver) {

            blockObservers.add(responseObserver);

            return new StreamObserver<Proto.Block>() {
                @Override
                public void onNext(Proto.Block protoBlock) {
                    long id = protoBlock.getHeader().getRawData().getIndex();
                    BlockHusk block = new BlockHusk(protoBlock);
                    log.debug("Received block id=[{}], hash={}", id, block.getHash());
                    BlockHusk newBlock = branchGroup.addBlock(block);
                    // ignore broadcast by other node's broadcast
                    if (newBlock == null) {
                        return;
                    }

                    for (StreamObserver<NetProto.Empty> observer : blockObservers) {
                        observer.onNext(EMPTY);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Broadcasting block failed: {}", t.getMessage());
                    blockObservers.remove(responseObserver);
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    blockObservers.remove(responseObserver);
                    responseObserver.onCompleted();
                }
            };
        }
    }

    static class PingPongImpl extends PingPongGrpc.PingPongImplBase {
        @Override
        public void play(Ping request, StreamObserver<Pong> responseObserver) {
            log.debug("Received " + request.getPing());
            Pong pong = Pong.newBuilder().setPong("Pong").build();
            responseObserver.onNext(pong);
            responseObserver.onCompleted();
        }
    }
}
