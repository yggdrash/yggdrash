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

package io.yggdrash.node.service;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.TransactionServiceGrpc;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GrpcTransactionServiceTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Mock
    private BranchGroup branchGroupMock;

    @Mock
    private BlockChainSyncManager syncManager;

    private Transaction tx;
    private ConsensusBlock block;
    private BranchId branchId;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new TransactionService(branchGroupMock, syncManager));

        tx = BlockChainTestUtils.createTransferTx();
        block = BlockChainTestUtils.genesisBlock();
        branchId = block.getBranchId();
    }

    @Test
    public void broadcastTx() {
        TransactionServiceGrpc.TransactionServiceStub asyncStub =
                TransactionServiceGrpc.newStub(grpcServerRule.getChannel());

        CommonProto.Empty empty = CommonProto.Empty.newBuilder().build();

        CompletableFuture<CommonProto.Empty> future = new CompletableFuture<>();

        StreamObserver<Proto.Transaction> requestObserver = asyncStub.broadcastTx(
                new StreamObserver<CommonProto.Empty>() {
                    @Override
                    public void onNext(CommonProto.Empty empty) {
                        future.complete(empty);
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onCompleted() {
                    }
                }
        );
        requestObserver.onNext(BlockChainTestUtils.createTransferTx().getInstance());
        requestObserver.onCompleted();

        if (future.isDone()) {
            try {
                assertEquals(empty, future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void syncTransaction() {
        // arrange
        when(branchGroupMock.getUnconfirmedTxs(branchId)).thenReturn(Collections.singletonList(tx));
        TransactionServiceGrpc.TransactionServiceBlockingStub blockingStub
                = TransactionServiceGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        CommonProto.SyncLimit syncLimit = CommonProto.SyncLimit.newBuilder().setBranch(branch).build();
        // act
        Proto.TransactionList list = blockingStub.syncTx(syncLimit);
        // assert
        assertEquals(1, list.getTransactionsCount());
    }

    @Test
    public void sendTx() {
        TransactionServiceGrpc.TransactionServiceBlockingStub blockingStub
                = TransactionServiceGrpc.newBlockingStub(grpcServerRule.getChannel());

        Transaction txImpl = BlockChainTestUtils.createTransferTx();
        Proto.Transaction tx = txImpl.getProtoTransaction();

        Proto.TransactionResponse res = blockingStub.sendTx(tx);
        Assert.assertEquals(1, res.getStatus()); //success
        Assert.assertEquals(0, res.getLogsCount());
        Assert.assertEquals(txImpl.getHash().toString(), res.getTxHash());
    }

}
