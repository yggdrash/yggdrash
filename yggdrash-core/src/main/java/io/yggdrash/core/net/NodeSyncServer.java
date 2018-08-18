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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeSyncServer {
    private static final Logger log = LoggerFactory.getLogger(NodeSyncServer.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();
    private Server server;
    private final NodeManager nodeManager;

    public NodeSyncServer(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void start(int port) throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new PingPongImpl())
                .addService(new BlockChainImpl(nodeManager))
                .build()
                .start();
        log.info("GRPC Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            NodeSyncServer.this.stop();
            System.err.println("*** server shut down");
        }));
        nodeManager.init();
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
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

    /**
     * The block chain rpc server implementation.
     */
    static class BlockChainImpl extends BlockChainGrpc.BlockChainImplBase {
        private static final Set<StreamObserver<NetProto.Empty>> txObservers =
                ConcurrentHashMap.newKeySet();
        private static final Set<StreamObserver<NetProto.Empty>> blockObservers =
                ConcurrentHashMap.newKeySet();
        private final NodeManager nodeManager;

        BlockChainImpl(NodeManager nodeManager) {
            this.nodeManager = nodeManager;
        }

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
            for (BlockHusk husk : nodeManager.getBlocks()) {
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
            for (TransactionHusk husk : nodeManager.getTransactionList()) {
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

            List<String> peerUriList = nodeManager.getPeerUriList();

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
            nodeManager.removePeer(peerRequest.getFrom());
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
                    TransactionHusk newTx = nodeManager.addTransaction(tx);
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
                    BlockHusk newBlock = nodeManager.addBlock(block);
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
}
