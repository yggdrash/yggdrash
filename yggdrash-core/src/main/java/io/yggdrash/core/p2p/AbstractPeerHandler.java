/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.p2p;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PeerServiceGrpc;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.TransactionServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public abstract class AbstractPeerHandler<T> implements PeerHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractPeerHandler.class);
    protected static final int DEFAULT_LIMIT = 10000;

    private final Peer peer;

    private final ManagedChannel channel;
    private PeerServiceGrpc.PeerServiceBlockingStub peerBlockingStub;
    private final TransactionServiceGrpc.TransactionServiceStub transactionAsyncStub;
    private final StreamObserver<CommonProto.Empty> emptyResponseStreamObserver;
    private StreamObserver<Proto.Transaction> broadcastTxRequestObserver;
    private boolean alive;

    public AbstractPeerHandler(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.peerBlockingStub = PeerServiceGrpc.newBlockingStub(channel);
        this.transactionAsyncStub = TransactionServiceGrpc.newStub(channel);
        this.emptyResponseStreamObserver =
                new StreamObserver<CommonProto.Empty>() {
                    @Override
                    public void onNext(CommonProto.Empty empty) {
                        log.debug("[PeerHandler] Empty Received");
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn("[PeerHandler] Broadcast Failed: {}", Status.fromThrowable(t));
                        alive = false;
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
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
        log.debug("Stop for peer={}", peer.getYnodeUri());

        if (channel != null) {
            channel.shutdown();
        }
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
    public Future<List<Transaction>> syncTx(BranchId branchId) {
        CommonProto.SyncLimit syncLimit = CommonProto.SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();

        log.debug("Requesting sync tx: branchId={}", branchId);

        CompletableFuture<List<Transaction>> husksCompletableFuture = new CompletableFuture<>();

        transactionAsyncStub.syncTx(syncLimit,
                new StreamObserver<Proto.TransactionList>() {
                    @Override
                    public void onNext(Proto.TransactionList txList) {
                        List<Transaction> txHusks = txList.getTransactionsList().stream()
                                .map(TransactionImpl::new).collect(Collectors.toList());
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

    @Override
    public void broadcastTx(Transaction tx) {
        log.debug("Broadcasting txs -> {}", peer.getYnodeUri());

        if (!alive) {
            alive = true;
            this.broadcastTxRequestObserver = transactionAsyncStub.broadcastTx(emptyResponseStreamObserver);
        }

        broadcastTxRequestObserver.onNext(tx.getInstance());
    }
}
