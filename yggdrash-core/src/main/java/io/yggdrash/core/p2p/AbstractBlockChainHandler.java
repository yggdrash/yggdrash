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
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.TransactionServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.yggdrash.common.config.Constants.TIMEOUT_TRANSACTION;

public abstract class AbstractBlockChainHandler<T> extends DiscoveryHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractBlockChainHandler.class);

    private final TransactionServiceGrpc.TransactionServiceStub transactionAsyncStub;
    private final StreamObserver<CommonProto.Empty> emptyResponseStreamObserver;
    private StreamObserver<Proto.Transaction> broadcastTxRequestObserver;

    public AbstractBlockChainHandler(ManagedChannel channel, Peer peer) {
        super(channel, peer);
        this.transactionAsyncStub = TransactionServiceGrpc.newStub(channel);
        this.emptyResponseStreamObserver =
                new StreamObserver<CommonProto.Empty>() {
                    @Override
                    public void onNext(CommonProto.Empty empty) {
                        log.debug("[PeerHandler] Empty Received");
                    }

                    @Override
                    public void onError(Throwable t) {
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
    public Future<List<Transaction>> syncTx(BranchId branchId) {
        CommonProto.SyncLimit syncLimit = CommonProto.SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();

        log.debug("Requesting sync tx: branchId={}", branchId);

        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();

        transactionAsyncStub.syncTx(syncLimit,
                new StreamObserver<Proto.TransactionList>() {
                    @Override
                    public void onNext(Proto.TransactionList protoTxList) {
                        List<Transaction> txList = protoTxList.getTransactionsList().stream()
                                .map(TransactionImpl::new).collect(Collectors.toList());
                        log.debug("[PeerHandler] TransactionList(size={}) Received", txList.size());

                        future.complete(txList);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        log.debug("[PeerHandler] Sync Tx Finished");
                        future.complete(Collections.emptyList());
                    }
                }
        );

        return future;
    }

    @Override
    public void broadcastTx(Transaction tx) {
        try {
            this.broadcastTxRequestObserver = transactionAsyncStub
                    .withDeadlineAfter(TIMEOUT_TRANSACTION, TimeUnit.SECONDS)
                    .broadcastTx(emptyResponseStreamObserver);
            broadcastTxRequestObserver.onNext(tx.getInstance());
            log.trace("Broadcasting tx={} to={}", tx.getHash().toString(), getPeer().getYnodeUri());
        } catch (Exception e) {
            log.trace("BroadcastingTx() is failed. {}", e.getMessage());
        }
    }
}
