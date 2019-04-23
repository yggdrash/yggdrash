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
        log.debug("Received syncTransaction request");
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : branchGroup.getUnconfirmedTxs(branchId)) {
            builder.addTransactions(tx.getInstance());
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
                log.debug("Received transaction: hash={}, {}", tx.getHash(), this);
                try {
                    branchGroup.addTransaction(tx);
                } catch (Exception e) {
                    log.warn(e.getMessage());
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
