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
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.NetProto.Empty;
import io.yggdrash.proto.NetProto.SyncLimit;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class GRpcClientChannel implements PeerClientChannel {

    private static final Logger log = LoggerFactory.getLogger(GRpcClientChannel.class);

    private final ManagedChannel channel;
    private final PingPongGrpc.PingPongBlockingStub blockingPingPongStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainStub asyncBlockChainStub;
    private final Peer peer;

    GRpcClientChannel(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    @VisibleForTesting
    GRpcClientChannel(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        blockingPingPongStub = PingPongGrpc.newBlockingStub(channel);
        blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        asyncBlockChainStub = BlockChainGrpc.newStub(channel);
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
        log.debug("Stop for peer=" + peer.getYnodeUri());
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public void stop(String ynodeUri) {
        disconnectPeer(ynodeUri);
        stop();
    }

    public void blockUtilShutdown() throws InterruptedException {
        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.MINUTES);
        }
    }

    @Override
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
    @Override
    public List<Proto.Block> syncBlock(BranchId branchId, long offset) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setOffset(offset)
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncBlock(syncLimit).getBlocksList();
    }

    /**
     * Sync transaction request
     *
     * @return the transaction list
     */
    @Override
    public List<Proto.Transaction> syncTransaction(BranchId branchId) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncTransaction(syncLimit).getTransactionsList();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction[] txs) {
        log.info("*** Broadcasting tx...");
        StreamObserver<Proto.Transaction> requestObserver =
                asyncBlockChainStub.broadcastTransaction(new StreamObserver<Empty>() {
                    @Override
                    public void onNext(NetProto.Empty empty) {
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

        for (Proto.Transaction tx : txs) {
            log.trace("Sending transaction: {}", tx);
            requestObserver.onNext(tx);
        }

        requestObserver.onCompleted();
    }

    @Override
    public void broadcastBlock(Proto.Block[] blocks) {
        log.info("*** Broadcasting blocks...");
        StreamObserver<Proto.Block> requestObserver =
                asyncBlockChainStub.broadcastBlock(new StreamObserver<NetProto.Empty>() {
                    @Override
                    public void onNext(NetProto.Empty empty) {
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

        for (Proto.Block block : blocks) {
            log.trace("Sending block: {}", block);
            requestObserver.onNext(block);
        }

        requestObserver.onCompleted();
    }

    @Override
    public List<String> requestPeerList(String ynodeUri, int limit) {
        if (ynodeUri.equals(peer.getYnodeUri())) {
            log.debug("ignore from me");
            return Collections.emptyList();
        }
        NetProto.PeerRequest request = NetProto.PeerRequest.newBuilder()
                .setFrom(ynodeUri).setLimit(limit).build();
        return blockingBlockChainStub.requestPeerList(request).getPeersList();
    }

    @Override
    public void disconnectPeer(String ynodeUri) {
        if (ynodeUri.equals(peer.getYnodeUri())) {
            log.debug("ignore from me");
            return;
        }
        log.info("Disconnect request peer=" + ynodeUri);
        NetProto.PeerRequest request = NetProto.PeerRequest.newBuilder()
                .setFrom(ynodeUri).build();
        blockingBlockChainStub.disconnectPeer(request);
    }
}
