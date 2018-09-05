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

import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.net.NodeSyncServer.BlockChainImpl;
import io.yggdrash.core.net.NodeSyncServer.PingPongImpl;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeSyncServerTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();
    @Mock
    private NodeManager nodeManagerMock;
    @Mock
    private BranchGroup branchGroupMock;

    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new PingPongImpl());
        grpcServerRule.getServiceRegistry().addService(new BlockChainImpl(nodeManagerMock));
        when(nodeManagerMock.getBranchGroup()).thenReturn(branchGroupMock);

        this.tx = TestUtils.createTxHusk();
        when(branchGroupMock.addTransaction(any())).thenReturn(tx);
        this.block = TestUtils.createGenesisBlockHusk();
        when(branchGroupMock.addBlock(any())).thenReturn(block);
    }

    @Test
    public void play() {
        PingPongGrpc.PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(
                grpcServerRule.getChannel());

        Pong pong = blockingStub.play(Ping.newBuilder().setPing("Ping").build());
        assertEquals("Pong", pong.getPong());
    }

    @Test
    public void requestPeerList() {
        when(nodeManagerMock.getPeerUriList()).thenReturn(Arrays.asList("a", "b", "c"));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        String ynodeUri = "ynode://75bff16c@localhost:9090";
        NetProto.PeerRequest.Builder builder
                = NetProto.PeerRequest.newBuilder().setFrom(ynodeUri);
        NetProto.PeerList response = blockingStub.requestPeerList(builder.build());
        assertEquals(3, response.getPeersCount());
        // limit test
        response = blockingStub.requestPeerList(builder.setLimit(2).build());
        assertEquals(2, response.getPeersCount());
    }

    @Test
    public void syncBlock() {
        Set<BlockHusk> blocks = new HashSet<>();
        blocks.add(block);
        when(branchGroupMock.getBlocks()).thenReturn(blocks);

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        NetProto.SyncLimit syncLimit
                = NetProto.SyncLimit.newBuilder().setOffset(0).build();
        Proto.BlockList list = blockingStub.syncBlock(syncLimit);
        assertEquals(1, list.getBlocksCount());
    }

    @Test
    public void syncTransaction() {
        when(branchGroupMock.getTransactionList()).thenReturn(Collections.singletonList(tx));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        NetProto.Empty empty = NetProto.Empty.getDefaultInstance();
        Proto.TransactionList list = blockingStub.syncTransaction(empty);
        assertEquals(1, list.getTransactionsCount());
    }

    @Test
    public void broadcastTransaction() throws Exception {
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<NetProto.Empty> responseObserver = StreamRecorder.create();
        StreamObserver<Proto.Transaction> requestObserver
                = stub.broadcastTransaction(responseObserver);

        requestObserver.onNext(tx.getInstance());
        requestObserver.onCompleted();
        assertNotNull(responseObserver.firstValue().get());
    }

    @Test
    public void broadcastBlock() throws Exception {
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<NetProto.Empty> responseObserver = StreamRecorder.create();
        StreamObserver<Proto.Block> requestObserver
                = stub.broadcastBlock(responseObserver);

        requestObserver.onNext(block.getInstance());
        requestObserver.onCompleted();

        NetProto.Empty firstResponse = responseObserver.firstValue().get();
        assertNotNull(firstResponse);
    }
}
