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

import com.google.gson.JsonObject;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.mapper.BlockMapper;
import io.yggdrash.core.mapper.TransactionMapper;
import io.yggdrash.core.net.NodeSyncServer.BlockChainImpl;
import io.yggdrash.core.net.NodeSyncServer.PingPongImpl;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeSyncServerTest {

    @Mock
    private NodeManager nodeManagerMock;

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    private Transaction tx;
    private Block block;

    @Before
    public void setUp() throws Exception {
        grpcServerRule.getServiceRegistry().addService(new PingPongImpl());
        grpcServerRule.getServiceRegistry().addService(new BlockChainImpl(nodeManagerMock));

        Account account = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(account, json);
        when(nodeManagerMock.addTransaction(any())).thenReturn(tx);

        BlockBody body = new BlockBody(Arrays.asList(new Transaction[] {tx}));

        BlockHeader header = new BlockHeader.Builder()
                .blockBody(body)
                .prevBlock(null)
                .build(account);
        this.block = new Block(header, body);
        when(nodeManagerMock.addBlock(any())).thenReturn(block);
    }

    @Test
    public void play() {
        PingPongGrpc.PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub
                (grpcServerRule.getChannel());

        Pong pong = blockingStub.play(Ping.newBuilder().setPing("Ping").build());
        assertEquals("Pong", pong.getPong());
    }

    @Test
    public void syncBlock() {
        Set<Block> blocks = new HashSet<>();
        blocks.add(block);
        when(nodeManagerMock.getBlocks()).thenReturn(blocks);

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        BlockChainProto.SyncLimit syncLimit
                = BlockChainProto.SyncLimit.newBuilder().setOffset(0).build();
        BlockChainProto.BlockList list = blockingStub.syncBlock(syncLimit);
        assertTrue(list.getBlocksList().size() == 1);
    }

    @Test
    public void syncTransaction() {
        when(nodeManagerMock.getTransactionList()).thenReturn(Arrays.asList(tx));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        BlockChainProto.Empty empty = BlockChainProto.Empty.newBuilder().build();
        BlockChainProto.TransactionList list = blockingStub.syncTransaction(empty);
        assertTrue(list.getTransactionsList().size() == 1);
    }

    @Test
    public void broadcastTransaction() throws Exception {
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<BlockChainProto.Transaction> responseObserver = StreamRecorder.create();
        StreamObserver<BlockChainProto.Transaction> requestObserver
                = stub.broadcastTransaction(responseObserver);

        BlockChainProto.Transaction request = TransactionMapper.transactionToProtoTransaction(tx);
        requestObserver.onNext(request);
        requestObserver.onCompleted();

        BlockChainProto.Transaction firstTxResponse = responseObserver.firstValue().get();
        assertEquals("{\"data\":\"TEST\"}", firstTxResponse.getData());
    }

    @Test
    public void broadcastBlock() throws Exception {
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<BlockChainProto.Block> responseObserver = StreamRecorder.create();
        StreamObserver<BlockChainProto.Block> requestObserver
                = stub.broadcastBlock(responseObserver);

        requestObserver.onNext(BlockMapper.blockToProtoBlock(block));
        requestObserver.onCompleted();

        BlockChainProto.Block firstResponse = responseObserver.firstValue().get();
        assertEquals(block.getHeader().getTimestamp(), firstResponse.getHeader().getTimestamp());
        assertEquals("{\"data\":\"TEST\"}", firstResponse.getData().getTrasactions(0).getData());
    }
}
