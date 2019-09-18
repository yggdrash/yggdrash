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

package io.yggdrash.node.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.TransactionServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@GrpcService
public class TransactionService extends TransactionServiceGrpc.TransactionServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private final BranchGroup branchGroup;

    @Autowired
    public TransactionService(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    /**
     * Sync transaction response
     *
     * @param syncLimit        the branch id to sync
     * @param responseObserver the observer response to the transaction list
     */
    @Override
    public void syncTx(CommonProto.SyncLimit syncLimit, StreamObserver<Proto.TransactionList> responseObserver) {
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        log.debug("Received syncTransaction request branchId={}", branchId);
        if (!branchGroup.getBranch(branchId).isFullSynced()) {
            log.debug("Not yet fullSynced.");
            return;
        }

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : branchGroup.getUnconfirmedTxs(branchId)) {
            builder.addTransactions(tx.getInstance());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * Return transactionResponse
     *
     * @param tx               transaction to transfer
     * @param responseObserver the observer response to a transactionResponse containing txHash, status, and logs
     */
    @Override
    public void sendTx(Proto.Transaction tx, StreamObserver<Proto.TransactionResponse> responseObserver) {
        BranchId branchId = BranchId.of(tx.getHeader().getChain().toByteArray());
        log.debug("Received sendTx request branchId={}", branchId);
        if (!branchGroup.getBranch(branchId).isFullSynced()) {
            log.debug("Not yet fullSynced.");
            return;
        }

        Proto.TransactionResponse.Builder builder = Proto.TransactionResponse.newBuilder();
        Transaction transaction = new TransactionImpl(tx);
        Map<String, List<String>> errorLogs = branchGroup.addTransaction(transaction);

        if (errorLogs.size() > 0) {
            log.debug("Received sendTx error occurred : {}", errorLogs);
            builder.setStatus(0);
        } else {
            builder.setStatus(1);
        }
        builder.setTxHash(transaction.getHash().toString());

        Proto.Log.Builder log = Proto.Log.newBuilder();
        for (String k : errorLogs.keySet()) {
            log.setCode(k);
            log.addAllMsg(errorLogs.get(k));
            builder.addLogs(log);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Proto.Transaction> broadcastTx(StreamObserver<CommonProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Transaction>() {
            @Override
            public void onNext(Proto.Transaction protoTx) {
                Transaction tx = new TransactionImpl(protoTx);
                try {
                    if (branchGroup.getBranch(tx.getBranchId()).isFullSynced()) {
                        branchGroup.addTransaction(tx);
                    } else {
                        log.debug("BroadcastTx() is failed. Not yet fullSynced.");
                        return;
                    }
                } catch (Exception e) {
                    log.warn("BroadcastTx() is failed. {}", e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Encountered error in broadcastTx: {}", Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast tx");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }
}
