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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GrpcBlockChainServiceTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Mock
    private BlockChainConsumer blockChainConsumerMock;

    private TransactionHusk tx;
    private BlockHusk block;
    private BranchId branchId;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new BlockChainService(blockChainConsumerMock));

        tx = BlockChainTestUtils.createTransferTxHusk();
        block = BlockChainTestUtils.genesisBlock();
        branchId = block.getBranchId();
    }

    @Test
    public void broadcastBlock() {
        BlockChainGrpc.BlockChainStub asyncStub = BlockChainGrpc.newStub(grpcServerRule.getChannel());

        NetProto.Empty empty = NetProto.Empty.newBuilder().build();
        Proto.Block block = BlockChainTestUtils.genesisBlock().getInstance();

        CompletableFuture<NetProto.Empty> husksCompletableFuture = new CompletableFuture<>();

        StreamObserver<Proto.Block> requestObserver = asyncStub.broadcastBlock(
                new StreamObserver<NetProto.Empty>() {
                    @Override
                    public void onNext(NetProto.Empty empty) {
                        husksCompletableFuture.complete(empty);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                }
        );

        requestObserver.onNext(block);
        requestObserver.onCompleted();

        if (husksCompletableFuture.isDone()) {
            try {
                assertEquals(empty, husksCompletableFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void broadcastTx() {
        BlockChainGrpc.BlockChainStub asyncStub = BlockChainGrpc.newStub(grpcServerRule.getChannel());

        NetProto.Empty empty = NetProto.Empty.newBuilder().build();
        Proto.Transaction tx = BlockChainTestUtils.createTransferTxHusk().getInstance();

        CompletableFuture<NetProto.Empty> husksCompletableFuture = new CompletableFuture<>();

        StreamObserver<Proto.Transaction> requestObserver = asyncStub.broadcastTx(
                new StreamObserver<NetProto.Empty>() {
                    @Override
                    public void onNext(NetProto.Empty empty) {
                        husksCompletableFuture.complete(empty);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                }
        );
        requestObserver.onNext(tx);
        requestObserver.onCompleted();

        if (husksCompletableFuture.isDone()) {
            try {
                assertEquals(empty, husksCompletableFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void syncBlock() {
        when(blockChainConsumerMock.syncBlock(branchId, 0L, 100L))
                .thenReturn(Collections.singletonList(block));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        NetProto.SyncLimit syncLimit =
                NetProto.SyncLimit.newBuilder().setOffset(0).setLimit(100).setBranch(branch).build();
        // act
        Proto.BlockList list = blockingStub.syncBlock(syncLimit);
        // assert
        assertEquals(1, list.getBlocksCount());
    }

    @Test
    public void syncBlockByPassingTheLimitSize() {
        // arrange
        List<BlockHusk> blockHuskList = BlockChainTestUtils.createBlockListFilledWithTx(block);

        when(blockChainConsumerMock.syncBlock(branchId, 0L, 100L))
                .thenReturn(blockHuskList);

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        NetProto.SyncLimit syncLimit =
                NetProto.SyncLimit.newBuilder().setOffset(0).setLimit(100).setBranch(branch).build();
        // act
        Proto.BlockList list = blockingStub.syncBlock(syncLimit);
        // assert
        assertEquals(90, list.getBlocksCount());
    }

    @Test
    public void syncTransaction() {
        // arrange
        when(blockChainConsumerMock.syncTx(branchId)).thenReturn(Collections.singletonList(tx));
        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        NetProto.SyncLimit syncLimit = NetProto.SyncLimit.newBuilder().setBranch(branch).build();
        // act
        Proto.TransactionList list = blockingStub.syncTx(syncLimit);
        // assert
        assertEquals(1, list.getTransactionsCount());
    }
}
