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
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.Peer;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GrpcDiscoveryServiceTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Mock
    private DiscoveryConsumer discoveryConsumerMock;

    private BranchId yggdrash;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new DiscoveryService(discoveryConsumerMock));
        yggdrash = TestConstants.yggdrash();

    }

    @Test
    public void play() {
        PeerGrpc.PeerBlockingStub blockingStub = PeerGrpc.newBlockingStub(
                grpcServerRule.getChannel());
        Peer requestPeer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        when(discoveryConsumerMock.play(requestPeer, "Ping")).thenReturn("Pong");

        Proto.PeerInfo peerInfo = Proto.PeerInfo.newBuilder()
                .setUrl(requestPeer.getYnodeUri())
                .build();

        Proto.Ping ping = Proto.Ping.newBuilder().setPing("Ping").setPeer(peerInfo).build();

        Proto.Pong pong = blockingStub.ping(ping);

        assertEquals("Pong", pong.getPong());
    }

    @Test
    public void findPeers() {
        PeerGrpc.PeerBlockingStub blockingStub = PeerGrpc.newBlockingStub(
                grpcServerRule.getChannel());

        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Proto.BestBlock bestBlock = Proto.BestBlock.newBuilder()
                .setBranch(ByteString.copyFrom(yggdrash.getBytes()))
                .setIndex(0).build();
        Proto.RequestPeer requestPeer = Proto.RequestPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .addBestBlocks(bestBlock)
                .build();

        Proto.PeerList peerList = blockingStub.findPeers(requestPeer);

        assertEquals(0, peerList.getPeersCount());
    }
}
