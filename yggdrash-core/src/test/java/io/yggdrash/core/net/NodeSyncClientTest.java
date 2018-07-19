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

import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
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
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NodeSyncClientTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Spy
    private PingPongGrpc.PingPongImplBase pingPongService;

    @Spy
    private BlockChainGrpc.BlockChainImplBase blockChainService;

    @Captor
    private ArgumentCaptor<Ping> pingRequestCaptor;

    @Captor
    private ArgumentCaptor<BlockChainProto.SyncLimit> syncLimitRequestCaptor;

    @Captor
    private ArgumentCaptor<BlockChainProto.Empty> emptyCaptor;

    @Captor
    private ArgumentCaptor<BlockChainProto.PeerRequest> peerRequestCaptor;

    private NodeSyncClient client;

    @Before
    public void setUp() {
        Peer peer = Peer.valueOf("ynode://75bff16c@localhost:9999");
        client = new NodeSyncClient(grpcServerRule.getChannel(), peer);
        grpcServerRule.getServiceRegistry().addService(pingPongService);
        grpcServerRule.getServiceRegistry().addService(blockChainService);
    }

    @Test
    public void getPeerYnodeUriTest() {
        NodeSyncClient client = new NodeSyncClient(Peer.valueOf("ynode://75bff16c@localhost:9090"));
        assertEquals("ynode://75bff16c@localhost:9090", client.getPeerYnodeUri());
    }

    @Test
    public void play() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<BlockChainProto.BlockList> argument = invocationOnMock.getArgument(1);
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
    public void syncBlock() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<BlockChainProto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).syncBlock(syncLimitRequestCaptor.capture(), any());

        long offset = 0;

        client.syncBlock(offset);

        verify(blockChainService).syncBlock(syncLimitRequestCaptor.capture(), any());

        assertEquals(offset, syncLimitRequestCaptor.getValue().getOffset());
    }

    @Test
    public void syncTransaction() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<BlockChainProto.Transaction> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).syncTransaction(emptyCaptor.capture(), any());

        client.syncTransaction();

        verify(blockChainService).syncTransaction(emptyCaptor.capture(), any());

        assertEquals("", emptyCaptor.getValue().toString());
    }

    @Test
    public void broadcastTransaction() {

        client.broadcastTransaction(NodeTestData.transactions());

        verify(blockChainService).broadcastTransaction(any());
    }

    @Test
    public void broadcastBlock() {

        client.broadcastBlock(NodeTestData.blocks());

        verify(blockChainService).broadcastBlock(any());
    }

    @Test
    public void requestPeerList() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<BlockChainProto.PeerRequest> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).requestPeerList(peerRequestCaptor.capture(), any());

        client.requestPeerList("ynode://75bff16c@localhost:9090", 10);

        verify(blockChainService).requestPeerList(peerRequestCaptor.capture(), any());

        assertEquals("ynode://75bff16c@localhost:9090", peerRequestCaptor.getValue().getFrom());
        assertEquals(10, peerRequestCaptor.getValue().getLimit());
    }

    @Test
    public void disconnectPeer() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<BlockChainProto.PeerRequest> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(blockChainService).disconnectPeer(peerRequestCaptor.capture(), any());

        client.disconnectPeer("ynode://75bff16c@localhost:9091");

        verify(blockChainService).disconnectPeer(peerRequestCaptor.capture(), any());

        assertEquals("ynode://75bff16c@localhost:9091", peerRequestCaptor.getValue().getFrom());
    }
}
