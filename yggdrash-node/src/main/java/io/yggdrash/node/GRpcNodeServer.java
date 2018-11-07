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
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.net.DiscoverTask;
import io.yggdrash.core.net.NodeManager;
import io.yggdrash.core.net.NodeServer;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.NodeInfo;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.PeerList;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.RequestPeer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GRpcNodeServer implements NodeServer, NodeManager {
    private static final Logger log = LoggerFactory.getLogger(GRpcNodeServer.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private BranchGroup branchGroup;

    private PeerGroup peerGroup;

    private Wallet wallet;

    private NodeStatus nodeStatus;

    private Server server;

    @Autowired
    public void setPeerGroup(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Autowired
    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    @Autowired
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    @Autowired
    public void setBranchGroup(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public void start(String host, int port) throws IOException {
        this.server = ServerBuilder.forPort(port)
                .addService(new PingPongImpl())
                .addService(new BlockChainImpl(peerGroup, branchGroup, nodeStatus))
                .addService(new PeerImpl())
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
        log.info("Destroy node=" + peerGroup.getOwner());
        peerGroup.destroy();
    }

    private void init() {
        log.info("Init node=" + peerGroup.getOwner());
        bootstrapping();
        nodeStatus.sync();
        for (BlockChain blockChain : branchGroup.getAllBranch()) {
            BranchId branchId = blockChain.getBranchId();
            syncBlockAndTransaction(branchId);
        }
        nodeStatus.up();
    }

    @Override
    public void generateBlock() {
        branchGroup.generateBlock(wallet);
    }

    @Override
    public String getNodeUri() {
        return peerGroup.getOwner().getYnodeUri();
    }

    @Override
    public void bootstrapping() {
        nodeDiscovery();
        peerGroup.getClosestPeers().forEach(p -> addPeerChannel(BranchId.stem(), p));
    }

    private void nodeDiscovery() {
        for (String ynodeUri : peerGroup.getBootstrappingSeedList()) {
            if (ynodeUri.equals(peerGroup.getOwner().getYnodeUri())) {
                continue;
            }
            Peer peer = Peer.valueOf(ynodeUri);
            PeerClientChannel client = new GRpcClientChannel(peer);
            log.info("Try connecting to SEED peer = {}", peer);

            try {
                List<NodeInfo> foundedPeerList
                        = client.findPeers(BranchId.stem(), peerGroup.getOwner());
                for (NodeInfo nodeInfo : foundedPeerList) {
                    peerGroup.addPeerByYnodeUri(BranchId.stem(), nodeInfo.getUrl());
                }
            } catch (Exception e) {
                log.error("Failed connecting to SEED peer = {}", peer);
                continue;
            }
            DiscoverTask discoverTask = new GrpcDiscoverTask(peerGroup);
            discoverTask.run();
            return;
        }
    }

    private void addPeerChannel(BranchId branchId, Peer peer) {
        if (peer == null || peerGroup.getOwner().equals(peer)) {
            return;
        }
        peerGroup.newPeerChannel(branchId, new GRpcClientChannel(peer));
    }

    private void syncBlockAndTransaction(BranchId branchId) {
        if (peerGroup.isChannelEmpty(branchId)) {
            return;
        }
        try {
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                BlockChainSync.syncTransaction(blockChain, peerGroup);
                BlockChainSync.syncBlock(blockChain, peerGroup);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    static class BlockChainImpl extends BlockChainGrpc.BlockChainImplBase {
        private final PeerGroup peerGroup;
        private final BranchGroup branchGroup;
        private final NodeStatus nodeStatus;

        BlockChainImpl(PeerGroup peerGroup, BranchGroup branchGroup, NodeStatus nodeStatus) {
            this.peerGroup = peerGroup;
            this.branchGroup = branchGroup;
            this.nodeStatus = nodeStatus;
        }

        private static final Set<StreamObserver<NetProto.Empty>> txObservers =
                ConcurrentHashMap.newKeySet();
        private static final Set<StreamObserver<NetProto.Empty>> blockObservers =
                ConcurrentHashMap.newKeySet();

        /**
         * Sync block response
         *
         * @param syncLimit        the start branch id, block index and limit to sync
         * @param responseObserver the observer response to the block list
         */
        @Override
        public void syncBlock(NetProto.SyncLimit syncLimit,
                              StreamObserver<Proto.BlockList> responseObserver) {
            long offset = syncLimit.getOffset();
            BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
            BlockChain blockChain = branchGroup.getBranch(branchId);
            Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
            if (blockChain == null) {
                log.warn("Invalid request for branchId={}", branchId);
                responseObserver.onNext(builder.build());
                responseObserver.onCompleted();
                return;
            }
            if (offset < 0) {
                offset = 0;
            }
            long limit = syncLimit.getLimit();
            log.debug("Synchronize block request offset={}, limit={}", offset, limit);

            for (int i = 0; i < limit; i++) {
                BlockHusk block = branchGroup.getBlockByIndex(branchId, offset++);
                if (block == null) {
                    break;
                }
                builder.addBlocks(block.getInstance());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        /**
         * Sync transaction response
         *
         * @param syncLimit        the branch id to sync
         * @param responseObserver the observer response to the transaction list
         */
        @Override
        public void syncTransaction(NetProto.SyncLimit syncLimit,
                                    StreamObserver<Proto.TransactionList> responseObserver) {
            log.debug("Synchronize tx request");

            BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
            Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
            for (TransactionHusk husk : branchGroup.getUnconfirmedTxs(branchId)) {
                builder.addTransactions(husk.getInstance());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
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
                    try {
                        branchGroup.addTransaction(tx);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    }
                    for (StreamObserver<NetProto.Empty> observer : txObservers) {
                        observer.onNext(EMPTY);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("broadcastTransaction onError={}", t.getMessage());
                    txObservers.remove(responseObserver);
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    log.warn("broadcastTransaction onCompleted. txObservers={}",
                            txObservers.size());
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
                    long id = ByteUtil.byteArrayToLong(
                            protoBlock.getHeader().getIndex().toByteArray());
                    BlockHusk block = new BlockHusk(protoBlock);
                    log.debug("Received block id=[{}], hash={}", id, block.getHash());
                    if (isValid(block)) {
                        try {
                            branchGroup.addBlock(block);
                        } catch (Exception e) {
                            log.warn(e.getMessage());
                        }
                    }
                    for (StreamObserver<NetProto.Empty> observer : blockObservers) {
                        observer.onNext(EMPTY);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("BroadcastBlock onError={}", t.getMessage());
                    blockObservers.remove(responseObserver);
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    blockObservers.remove(responseObserver);
                    log.debug("BroadcastBlock onCompleted. blockObserver={}",
                            blockObservers.size());
                    responseObserver.onCompleted();
                }

                private boolean isValid(BlockHusk block) {
                    BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
                    if (!nodeStatus.isUpStatus()) {
                        log.trace("Ignore broadcast block");
                        return false;
                    } else if (blockChain == null) {
                        return false;
                    } else if (blockChain.getLastIndex() + 1 < block.getIndex()) {
                        log.info("Sync request latest block id=[{}]", blockChain.getLastIndex());
                        nodeStatus.sync();
                        BlockChainSync.syncBlock(blockChain, peerGroup);
                        nodeStatus.up();
                        return false;
                    }
                    return true;
                }
            };
        }
    }

    public class GrpcDiscoverTask extends DiscoverTask {
        public GrpcDiscoverTask(PeerGroup peerGroup) {
            super(peerGroup);
        }

        @Override
        public PeerClientChannel getClient(Peer peer) {
            return new GRpcClientChannel(peer);
        }
    }

    class PeerImpl extends PeerGrpc.PeerImplBase {
        @Override
        public void findPeers(RequestPeer request, StreamObserver<PeerList> responseObserver) {
            log.debug("Request Peer => "
                    + request.getPubKey() + "@" + request.getIp() + ":" + request.getPort());
            Peer peer = Peer.valueOf(request.getPubKey(), request.getIp(), request.getPort());
            List<String> list = peerGroup.getPeers(BranchId.of(request.getBranchId()), peer);
            PeerList.Builder peerListBuilder = PeerList.newBuilder();
            for (String url : list) {
                peerListBuilder.addNodes(NodeInfo.newBuilder().setUrl(url).build());
            }
            PeerList peerList = peerListBuilder.build();
            responseObserver.onNext(peerList);
            responseObserver.onCompleted();
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

    private static class BlockChainSync {

        static void syncBlock(BlockChain blockChain, PeerGroup peerGroup) {
            List<BlockHusk> blockList;
            do {
                blockList = peerGroup.syncBlock(blockChain.getBranchId(),
                        blockChain.getLastIndex() + 1);
                for (BlockHusk block : blockList) {
                    blockChain.addBlock(block, false);
                }
            } while (!blockList.isEmpty());
        }

        static void syncTransaction(BlockChain blockChain, PeerGroup peerGroup) {
            List<TransactionHusk> txList = peerGroup.syncTransaction(blockChain.getBranchId());
            for (TransactionHusk tx : txList) {
                try {
                    blockChain.addTransaction(tx);
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }
}
