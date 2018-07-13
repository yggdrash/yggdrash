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

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NodeSyncClient {

    public static final Logger log = LoggerFactory.getLogger(NodeSyncClient.class);

    private final ManagedChannel channel;
    private final PingPongGrpc.PingPongBlockingStub blockingStub;
    private final BlockChainGrpc.BlockChainStub asyncStub;

    public NodeSyncClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    NodeSyncClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PingPongGrpc.newBlockingStub(channel);
        asyncStub = BlockChainGrpc.newStub(channel);
    }

    public void stop() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    void blockUtilShutdown() throws InterruptedException {
        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.MINUTES);
        }
    }

    /**
     * Sync block request
     *
     * @param offset the start block index to sync
     * @return the block list
     */
    public List<BlockChainProto.Block> syncBlock(long offset) {
        SyncLimit syncLimit = SyncLimit.newBuilder().setOffset(offset).build();
        return BlockChainGrpc.newBlockingStub(channel).syncBlock(syncLimit).getBlocksList();
    }

    /**
     * Sync transaction request
     *
     * @return the transaction list
     */
    public List<BlockChainProto.Transaction> syncTransaction() {
        Empty empty = Empty.newBuilder().build();
        return BlockChainGrpc.newBlockingStub(channel).syncTransaction(empty).getTransactionsList();
    }

    public void ping(String message) {
        Ping request = Ping.newBuilder().setPing(message).build();
        try {
            Pong pong = blockingStub.play(request);
            log.debug(pong.getPong());
        } catch (Exception e) {
            log.info("Ping retrying...");
        }
    }

    public void broadcastTransaction(BlockChainProto.Transaction[] txs) {
        log.info("*** Broadcasting tx...");
        StreamObserver<BlockChainProto.Transaction> requestObserver =
                asyncStub.broadcastTransaction(new StreamObserver<BlockChainProto.Transaction>() {
                    @Override
                    public void onNext(BlockChainProto.Transaction tx) {
                        log.trace("Got transaction: {}", tx);
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("Broadcast transaction failed: {}",
                                Status.fromThrowable(t).getCode());
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Finished broadcasting");
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
                asyncStub.broadcastBlock(new StreamObserver<BlockChainProto.Block>() {
                    @Override
                    public void onNext(BlockChainProto.Block block) {
                        log.trace("Got block: {}", block);
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("Broadcast block failed: {}", Status.fromThrowable(t).getCode());
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Finished broadcasting");
                    }
                });

        for (BlockChainProto.Block block : blocks) {
            log.trace("Sending block: {}", block);
            requestObserver.onNext(block);
        }

        requestObserver.onCompleted();
    }
}
