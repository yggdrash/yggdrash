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

import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GRpcClientChannelTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Spy
    private PingPongGrpc.PingPongImplBase pingPongService;

    @Spy
    private BlockChainGrpc.BlockChainImplBase blockChainService;

    @Captor
    private ArgumentCaptor<Ping> pingRequestCaptor;

    @Captor
    private ArgumentCaptor<Proto.Block> blockArgumentCaptor;

    @Captor
    private ArgumentCaptor<Proto.Transaction> transactionArgumentCaptor;

    @Captor
    private ArgumentCaptor<NetProto.SyncLimit> syncLimitRequestCaptor;

    private GRpcClientChannel client;

    @Before
    public void setUp() {
        Peer peer = Peer.valueOf("ynode://75bff16c@localhost:9999");
        client = new GRpcClientChannel(grpcServerRule.getChannel(), peer);
        grpcServerRule.getServiceRegistry().addService(pingPongService);
        grpcServerRule.getServiceRegistry().addService(blockChainService);
    }

    @Test
    public void getPeerYnodeUriTest() {
        GRpcClientChannel client =
                new GRpcClientChannel(Peer.valueOf("ynode://75bff16c@localhost:32918"));
        assertEquals("ynode://75bff16c@localhost:32918", client.getPeer().getYnodeUri());
    }

    @Test
    public void play() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(pingPongService).play(pingRequestCaptor.capture(), any());

        String ping = "Ping";

        client.ping(ping);

        verify(pingPongService).play(pingRequestCaptor.capture(), any());

        assertEquals(ping, pingRequestCaptor.getValue().getPing());
    }

    @Test
    public void broadcastBlock() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.Block> blockStreamObserver = invocationOnMock.getArgument(1);
            blockStreamObserver.onNext(null);
            blockStreamObserver.onCompleted();
            return null;
        }).when(blockChainService).broadcastBlock(blockArgumentCaptor.capture(), any());

        client.broadcastBlock(TestUtils.sampleBlocks());

        verify(blockChainService, times(3))
                .broadcastBlock(blockArgumentCaptor.capture(), any());

        assertEquals(6, blockArgumentCaptor.getAllValues().size());
    }

    @Test
    public void broadcastTransaction() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.Transaction> transactionStreamObserver
                    = invocationOnMock.getArgument(1);
            transactionStreamObserver.onNext(null);
            transactionStreamObserver.onCompleted();
            return null;
        }).when(blockChainService).broadcastTransaction(transactionArgumentCaptor.capture(), any());

        client.broadcastTransaction(TestUtils.sampleTxs());

        verify(blockChainService, times(3))
                .broadcastTransaction(transactionArgumentCaptor.capture(), any());

        assertEquals(6, transactionArgumentCaptor.getAllValues().size());
    }

    @Test
    public void syncBlock() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).syncBlock(syncLimitRequestCaptor.capture(), any());

        long offset = 0;

        client.syncBlock(BranchId.NULL, offset);

        verify(blockChainService).syncBlock(syncLimitRequestCaptor.capture(), any());

        assertEquals(offset, syncLimitRequestCaptor.getValue().getOffset());
    }

    @Test
    public void syncTransaction() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.Transaction> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).syncTransaction(syncLimitRequestCaptor.capture(), any());

        client.syncTransaction(BranchId.NULL);

        verify(blockChainService).syncTransaction(syncLimitRequestCaptor.capture(), any());

        BranchId branch = BranchId.of(syncLimitRequestCaptor.getValue().getBranch().toByteArray());
        assertEquals(BranchId.NULL, branch);
    }

    /*
    @Test
    public void broadcastTransaction() {
        client.broadcastTransaction(TestUtils.sampleTxs());
        verify(blockChainService).broadcastTransaction(any(), any());
    }

    @Test
    public void broadcastBlock() {
        client.broadcastBlock(TestUtils.sampleBlocks());
        verify(blockChainService).broadcastBlock(any(), any());
    }
    */
}
