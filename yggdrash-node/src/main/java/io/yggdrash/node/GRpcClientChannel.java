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

import java.util.List;

class GRpcClientChannel implements PeerClientChannel {

    private static final Logger log = LoggerFactory.getLogger(GRpcClientChannel.class);
    private static final int DEFAULT_LIMIT = 10000;

    private final ManagedChannel channel;
    private final PingPongGrpc.PingPongBlockingStub blockingPingPongStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainStub asyncBlockChainStub;
    private final Peer peer;

    GRpcClientChannel(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    GRpcClientChannel(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.blockingPingPongStub = PingPongGrpc.newBlockingStub(channel);
        this.blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        this.asyncBlockChainStub = BlockChainGrpc.newStub(channel);
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
                .setLimit(DEFAULT_LIMIT)
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
        log.info("*** Broadcasting blocks -> {}", peer.getHost() + ":" + peer.getPort());
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
}
