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

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.NetProto.SyncLimit;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class GRpcPeerHandler implements PeerHandler {

    private static final Logger log = LoggerFactory.getLogger(GRpcPeerHandler.class);
    private static final int DEFAULT_LIMIT = 10000;

    private final Peer peer;

    private final ManagedChannel channel;
    private final PeerGrpc.PeerBlockingStub peerBlockingStub;
    private final BlockChainGrpc.BlockChainStub blockChainAsyncStub;
    private final StreamObserver<NetProto.Empty> emptyResponseStreamObserver;
    private StreamObserver<Proto.Block> broadcastBlockRequestObserver;
    private StreamObserver<Proto.Transaction> broadcastTxRequestObserver;

    GRpcPeerHandler(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    GRpcPeerHandler(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.peerBlockingStub = PeerGrpc.newBlockingStub(channel);
        this.blockChainAsyncStub = BlockChainGrpc.newStub(channel);
        this.emptyResponseStreamObserver =
                new StreamObserver<NetProto.Empty>() {
                    @Override
                    public void onNext(NetProto.Empty empty) {
                        log.debug("[PeerHandler] Empty Received");
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("[PeerHandler] Broadcast Failed: {}", Status.fromThrowable(t));
                    }

                    @Override
                    public void onCompleted() {
                        log.debug("[PeerHandler] Broadcast Finished");
                        // Server destroyed
                        channel.shutdown();
                    }
                };
    }

    @Override
    public List<Peer> findPeers(BranchId branchId, Peer peer) {
        Proto.TargetPeer targetPeer = Proto.TargetPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .build();
        return peerBlockingStub.findPeers(targetPeer).getPeersList().stream()
                .map(peerInfo -> Peer.valueOf(peerInfo.getUrl()))
                .collect(Collectors.toList());
    }

    @Override
    public String ping(BranchId branchId, Peer owner, String message) {
        Proto.Ping request = Proto.Ping.newBuilder().setPing(message)
                .setFrom(owner.getYnodeUri())
                .setTo(peer.getYnodeUri())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .build();
        return peerBlockingStub.ping(request).getPong();
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
        log.debug("Stop for peer=" + peer.getYnodeUri());

        if (isAlive()) {
            if (broadcastBlockRequestObserver != null) {
                try {
                    broadcastBlockRequestObserver.onCompleted();
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
            if (broadcastTxRequestObserver != null) {
                try {
                    broadcastTxRequestObserver.onCompleted();
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
            channel.shutdown();
        }
    }

    @Override
    public Future<List<BlockHusk>> syncBlock(BranchId branchId, long offset) {
        log.debug("Requesting sync block: branchId={}, offset={}", branchId, offset);

        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setOffset(offset)
                .setLimit(DEFAULT_LIMIT)
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .setFrom(peer.getYnodeUri()).build();

        CompletableFuture<List<BlockHusk>> husksCompletableFuture = new CompletableFuture<>();

        blockChainAsyncStub.syncBlock(syncLimit,
                new StreamObserver<Proto.BlockList>() {
                    @Override
                    public void onNext(Proto.BlockList blockList) {
                        List<BlockHusk> blockHusks = blockList.getBlocksList().stream()
                                .map(BlockHusk::new).collect(Collectors.toList());
                        log.debug("[PeerHandler] BlockList(size={}) Received", blockHusks.size());
                        husksCompletableFuture.complete(blockHusks);
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.debug("[PeerHandler] Sync Block Failed: {}", Status.fromThrowable(t));
                        husksCompletableFuture.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        log.debug("[PeerHandler] Sync Block Finished");
                        husksCompletableFuture.complete(Collections.emptyList());
                    }
                }
        );

        return husksCompletableFuture;
    }

    @Override
    public Future<List<TransactionHusk>> syncTx(BranchId branchId) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();

        log.debug("Requesting sync tx: branchId={}", branchId);

        CompletableFuture<List<TransactionHusk>> husksCompletableFuture = new CompletableFuture<>();

        blockChainAsyncStub.syncTx(syncLimit,
                new StreamObserver<Proto.TransactionList>() {
                    @Override
                    public void onNext(Proto.TransactionList txList) {
                        List<TransactionHusk> txHusks = txList.getTransactionsList().stream()
                                .map(TransactionHusk::new).collect(Collectors.toList());
                        log.debug("[PeerHandler] TransactionList(size={}) Received", txHusks.size());

                        husksCompletableFuture.complete(txHusks);
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.debug("[PeerHandler] Sync Block Failed: {}", Status.fromThrowable(t));
                        husksCompletableFuture.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        log.debug("[PeerHandler] Sync Tx Finished");
                        husksCompletableFuture.complete(Collections.emptyList());
                    }
                }
        );

        return husksCompletableFuture;
    }

    // When we send a (single) block to the server and get back a (single) empty.
    // Use the asynchronous stub for this method.
    @Override
    public void broadcastBlock(BlockHusk blockHusk) {
        log.debug("Broadcasting blocks -> {}", peer.getHost() + ":" + peer.getPort());

        if (this.broadcastBlockRequestObserver == null) {
            this.broadcastBlockRequestObserver =
                    blockChainAsyncStub.broadcastBlock(emptyResponseStreamObserver);
        }

        broadcastBlockRequestObserver.onNext(blockHusk.getInstance());
    }

    @Override
    public void broadcastTx(TransactionHusk txHusk) {
        log.debug("Broadcasting txs -> {}", txHusk.getHash());

        if (this.broadcastTxRequestObserver == null) {
            this.broadcastTxRequestObserver = blockChainAsyncStub.broadcastTx(emptyResponseStreamObserver);
        }

        broadcastTxRequestObserver.onNext(txHusk.getInstance());
    }

    private boolean isAlive() {
        return channel != null && !channel.isTerminated() && !channel.isShutdown();
    }
}
