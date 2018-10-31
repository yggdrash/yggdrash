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

package io.yggdrash.node;

import com.google.protobuf.ByteString;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerGroup;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GRpcNodeServerTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();
    @Mock
    private PeerGroup peerGroupMock;
    @Mock
    private BranchGroup branchGroupMock;
    @Mock
    private NodeStatus nodeStatus;

    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new GRpcNodeServer.PingPongImpl());
        grpcServerRule.getServiceRegistry().addService(new GRpcNodeServer.BlockChainImpl(
                peerGroupMock, branchGroupMock, nodeStatus)
        );

        this.tx = TestUtils.createTransferTxHusk();
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
    public void syncBlock() {
        Set<BlockHusk> blocks = new HashSet<>();
        blocks.add(block);
        when(branchGroupMock.getBlockByIndex(BranchId.stem(), 0L)).thenReturn(block);
        when(branchGroupMock.getBranch(any())).thenReturn(TestUtils.createBlockChain(false));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(BranchId.stem().getBytes());
        NetProto.SyncLimit syncLimit = NetProto.SyncLimit.newBuilder().setOffset(0).setLimit(10000)
                .setBranch(branch).build();
        Proto.BlockList list = blockingStub.syncBlock(syncLimit);
        assertEquals(1, list.getBlocksCount());
    }

    @Test
    public void syncTransaction() {
        when(branchGroupMock.getRecentTxs(BranchId.stem()))
                .thenReturn(Collections.singletonList(tx));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(BranchId.stem().getBytes());
        NetProto.SyncLimit syncLimit
                = NetProto.SyncLimit.newBuilder().setBranch(branch).build();
        Proto.TransactionList list = blockingStub.syncTransaction(syncLimit);
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
        when(nodeStatus.isUpStatus()).thenReturn(true);
        when(branchGroupMock.getBranch(any())).thenReturn(TestUtils.createBlockChain(false));
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
