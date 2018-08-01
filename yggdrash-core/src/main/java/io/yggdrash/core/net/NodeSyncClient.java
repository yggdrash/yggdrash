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

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.BlockChainProto.Empty;
import io.yggdrash.proto.BlockChainProto.SyncLimit;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NodeSyncClient {

    public static final Logger log = LoggerFactory.getLogger(NodeSyncClient.class);

    private final ManagedChannel channel;
    private final PingPongGrpc.PingPongBlockingStub blockingPingPongStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainStub asyncBlockChainStub;
    private final Peer peer;

    public NodeSyncClient(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    @VisibleForTesting
    NodeSyncClient(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        blockingPingPongStub = PingPongGrpc.newBlockingStub(channel);
        blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        asyncBlockChainStub = BlockChainGrpc.newStub(channel);
    }

    public Peer getPeer() {
        return peer;
    }

    public void stop() {
        log.debug("Stop for peer=" + peer.getYnodeUri());
        if (channel != null) {
            channel.shutdown();
        }
    }

    public void stop(String ynodeUri) {
        disconnectPeer(ynodeUri);
        stop();
    }

    void blockUtilShutdown() throws InterruptedException {
        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.MINUTES);
        }
    }

    public Pong ping(String message) {
        Ping request = Ping.newBuilder().setPing(message).build();
        return blockingPingPongStub.play(request);
    }

    /**
     * Sync block request
     *
     * @param offset the start block index to sync
     * @return the block list
     */
    public List<BlockChainProto.Block> syncBlock(long offset) {
        SyncLimit syncLimit = SyncLimit.newBuilder().setOffset(offset).build();
        return blockingBlockChainStub.syncBlock(syncLimit).getBlocksList();
    }

    /**
     * Sync transaction request
     *
     * @return the transaction list
     */
    public List<BlockChainProto.Transaction> syncTransaction() {
        Empty empty = Empty.getDefaultInstance();
        return blockingBlockChainStub.syncTransaction(empty).getTransactionsList();
    }

    public void broadcastTransaction(BlockChainProto.Transaction[] txs) {
        log.info("*** Broadcasting tx...");
        StreamObserver<BlockChainProto.Transaction> requestObserver =
                asyncBlockChainStub.broadcastTransaction(new StreamObserver<Empty>() {
                    @Override
                    public void onNext(BlockChainProto.Empty empty) {
                        log.trace("Got response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("Broadcast transaction failed: {}",
                                Status.fromThrowable(t).getCode());
                    }

                    @Override
                    public void onCompleted() {
                        log.trace("Finished broadcasting");
                    }
                });

        for (BlockChainProto.Transaction tx : txs) {
            log.trace("Sending transaction: {}", tx);
            requestObserver.onNext(tx);
        }

        requestObserver.onCompleted();
    }

    public void broadcastBlock(BlockChainProto.Block[] blocks) {
        log.info("*** Broadcasting blocks...");
        StreamObserver<BlockChainProto.Block> requestObserver =
                asyncBlockChainStub.broadcastBlock(new StreamObserver<BlockChainProto.Empty>() {
                    @Override
                    public void onNext(BlockChainProto.Empty empty) {
                        log.trace("Got response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("Broadcast block failed: {}", Status.fromThrowable(t).getCode());
                    }

                    @Override
                    public void onCompleted() {
                        log.trace("Finished broadcasting");
                    }
                });

        for (BlockChainProto.Block block : blocks) {
            log.trace("Sending block: {}", block);
            requestObserver.onNext(block);
        }

        requestObserver.onCompleted();
    }

    public List<String> requestPeerList(String ynodeUri, int limit) {
        if (ynodeUri.equals(peer.getYnodeUri())) {
            log.debug("ignore from me");
            return Collections.emptyList();
        }
        BlockChainProto.PeerRequest request = BlockChainProto.PeerRequest.newBuilder()
                .setFrom(ynodeUri).setLimit(limit).build();
        return blockingBlockChainStub.requestPeerList(request).getPeersList();
    }

    public void disconnectPeer(String ynodeUri) {
        if (ynodeUri.equals(peer.getYnodeUri())) {
            log.debug("ignore from me");
            return;
        }
        log.info("Disconnect request peer=" + ynodeUri);
        BlockChainProto.PeerRequest request = BlockChainProto.PeerRequest.newBuilder()
                .setFrom(ynodeUri).build();
        blockingBlockChainStub.disconnectPeer(request);
    }
}
