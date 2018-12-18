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
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.DiscoverTask;
import io.yggdrash.core.net.NodeManager;
import io.yggdrash.core.net.NodeServer;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.wallet.Wallet;
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
import java.util.ArrayList;
import java.util.List;

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
        syncBlockAndTransaction();
        nodeStatus.up();
    }

    @Override
    public void generateBlock(BranchId branchId) {
        branchGroup.generateBlock(wallet, branchId);
    }

    @Override
    public List<BranchId> getActiveBranchIdList() {
        return new ArrayList<>(branchGroup.getAllBranchId());
    }

    @Override
    public String getNodeUri() {
        return peerGroup.getOwner().getYnodeUri();
    }

    @Override
    public void bootstrapping() {
        if (peerGroup.getOwner().isLocal()) {
            log.info("Ignore bootstrapping peer={}", peerGroup.getOwner().toAddress());
            return;
        }

        for (BranchId branchId : branchGroup.getAllBranchId()) {
            log.debug("bootstrapping :: branchId => " + branchId);
            nodeDiscovery(branchId);
            for (Peer peer : peerGroup.getClosestPeers(branchId)) {
                if (peerGroup.isMaxChannel(branchId)) {
                    break;
                }
                peerGroup.newPeerChannel(branchId, new GRpcClientChannel(peer));
            }
        }
    }

    private void nodeDiscovery(BranchId branchId) {
        for (String ynodeUri : peerGroup.getBootstrappingSeedList(branchId)) {
            String ynodeUriWithoutPubKey = peerGroup.getOwner().getYnodeUri()
                    .substring(ynodeUri.indexOf("@"));
            if (ynodeUri.contains(ynodeUriWithoutPubKey)) {
                continue;
            }
            Peer peer = Peer.valueOf(ynodeUri);
            PeerClientChannel client = new GRpcClientChannel(peer);
            log.info("Try connecting to SEED peer = {}", peer);

            try {
                List<NodeInfo> foundedPeerList
                        = client.findPeers(branchId, peerGroup.getOwner());
                for (NodeInfo nodeInfo : foundedPeerList) {
                    peerGroup.addPeerByYnodeUri(branchId, nodeInfo.getUrl());
                }
            } catch (Exception e) {
                log.error("Failed connecting to SEED peer = {}", peer);
                continue;
            } finally {
                client.stop();
            }
            DiscoverTask discoverTask = new GrpcDiscoverTask(peerGroup, branchId);
            discoverTask.run();
            return;
        }
    }

    public boolean isSeedPeer() {
        List<String> seedPeerList = peerGroup.getSeedPeerList();
        String nodeUriWithoutPubKey = getNodeUri();
        nodeUriWithoutPubKey = nodeUriWithoutPubKey.substring(nodeUriWithoutPubKey.indexOf("@"));
        for (String seedPeer : seedPeerList) {
            if (seedPeer.contains(nodeUriWithoutPubKey)) {
                log.info("* I'm the SeedPeer!");
                return true;
            }
        }
        return false;
    }

    private void syncBlockAndTransaction() {
        try {
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                if (peerGroup.isChannelEmpty(blockChain.getBranchId())) {
                    continue;
                }
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
        public void broadcastBlock(Proto.Block request,
                                   StreamObserver<NetProto.Empty> responseObserver) {
            long id = ByteUtil.byteArrayToLong(
                    request.getHeader().getIndex().toByteArray());
            BlockHusk block = new BlockHusk(request);
            log.debug("Received block id=[{}], hash={}", id, block.getHash());
            if (isBlockValid(block)) {
                try {
                    branchGroup.addBlock(block, true);
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
        }

        @Override
        public void broadcastTransaction(Proto.Transaction request,
                                         StreamObserver<NetProto.Empty> responseObserver) {
            log.debug("Received transaction: {}", request);
            TransactionHusk tx = new TransactionHusk(request);
            try {
                branchGroup.addTransaction(tx);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
        }

        private boolean isBlockValid(BlockHusk block) {
            BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
            if (!nodeStatus.isUpStatus()) {
                log.trace("Ignore broadcast block");
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
    }

    public class GrpcDiscoverTask extends DiscoverTask {
        GrpcDiscoverTask(PeerGroup peerGroup, BranchId branchId) {
            super(peerGroup, branchId);
        }

        @Override
        public PeerClientChannel getClient(Peer peer) {
            return new GRpcClientChannel(peer);
        }
    }

    class PeerImpl extends PeerGrpc.PeerImplBase {
        @Override
        public void findPeers(RequestPeer request, StreamObserver<PeerList> responseObserver) {
            BranchId branchId = BranchId.of(request.getBranchId());
            PeerList.Builder peerListBuilder = PeerList.newBuilder();
            if (!branchGroup.containsBranch(branchId)) {
                PeerList peerList = peerListBuilder.build();
                responseObserver.onNext(peerList);
                responseObserver.onCompleted();
                return;
            }

            Peer peer = Peer.valueOf(request.getPubKey(), request.getIp(), request.getPort());
            // 현재 연결된 채널들의 버킷 아이디, 새로들어온 requestPeer 의 버킷아이디 로그
            log.debug("Received findPeers peer={}, branch={}, bucketId={}",
                    peer.toAddress(), branchId, peerGroup.logBucketIdOf(branchId, peer));
            peerGroup.logBucketIdOf(branchId);

            List<String> list = peerGroup.getPeers(BranchId.of(request.getBranchId()), peer);
            for (String url : list) {
                peerListBuilder.addNodes(NodeInfo.newBuilder().setUrl(url).build());
            }
            PeerList peerList = peerListBuilder.build();
            responseObserver.onNext(peerList);
            responseObserver.onCompleted();

            try {
                if (!peerGroup.isMaxChannel(branchId)) {
                    peerGroup.newPeerChannel(branchId, new GRpcClientChannel(peer));
                } else {
                    // maxPeer 를 넘은경우부터 거리 계산된 peerTable 을 기반으로 peerChannel 업데이트
                    if (peerGroup.isClosePeer(branchId, peer)) {
                        peerGroup.reloadPeerChannel(branchId, new GRpcClientChannel(peer));
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to connect {} -> {}", peerGroup.getOwner().toAddress(),
                        peer.toAddress());
            }
        }

        @Override
        public void broadcastConsensus(
                RequestPeer request, StreamObserver<RequestPeer> responseObserver) {
            responseObserver.onNext(request);
            responseObserver.onCompleted();

            // RequestPeer 는 블록을 생성할 피어 (네트워크 상에서 블록을 생성할 피어의 순서를 지정하기 위함)
            String selectedPeer = Peer.valueOf(
                    request.getPubKey(), request.getIp(), request.getPort()).getYnodeUri();
            String owner = getNodeUri();

            // owner 가 selectedPeer 인 경우 블록 생성
            if (owner.equals(selectedPeer)) {
                generateBlock(BranchId.of(request.getBranchId()));
            }
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
