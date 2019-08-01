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

package io.yggdrash.validator.service.node;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.consensus.ConsensusService;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.TransactionServiceGrpc;
import io.yggdrash.validator.service.ConsensusClientStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TransactionServiceStub extends TransactionServiceGrpc.TransactionServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceStub.class);
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain blockChain;
    private final ConsensusService consensusService;

    public TransactionServiceStub(ConsensusBlockChain blockChain, ConsensusService consensusService) {
        this.blockChain = blockChain;
        this.consensusService = consensusService;
    }

    @Override
    public void syncTx(CommonProto.SyncLimit syncLimit, StreamObserver<Proto.TransactionList> responseObserver) {
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        log.debug("Received syncTransaction request branchId={}", branchId);

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        if (branchId.equals(blockChain.getBranchId())) {
            List<Transaction> txList = (List<Transaction>) blockChain.getBlockChainManager().getUnconfirmedTxs();
            for (Transaction tx : txList) {
                builder.addTransactions(tx.getInstance());
            }
        } else {
            log.warn("Wrong branch request branchId={}, blockChainBranchId={}", branchId, blockChain.getBranchId());
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
                log.trace("Received transaction: hash={}", tx.getHash());
                if (tx.getBranchId().equals(blockChain.getBranchId())
                        && blockChain.getBlockChainManager().verify(tx) == BusinessError.VALID.toValue()) {
                    blockChain.getBlockChainManager().addTransaction(tx);
                    multicastTransaction(protoTx);
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

    private void multicastTransaction(Proto.Transaction protoTx) {
        for (Object object : consensusService.getTotalValidatorMap().values()) {
            ConsensusClientStub client = (ConsensusClientStub) object;
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastTransaction(protoTx);
                } catch (Exception e) {
                    log.debug("multicastTransaction failed: {} ", client.getId());
                }
            }
        }
    }
}
