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
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.PeerGrpc;
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
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GRpcPeerHandlerTest extends TestConstants.CiTest {

    private static final Peer TARGET = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Spy
    private PeerGrpc.PeerImplBase peerService;

    @Spy
    private BlockChainGrpc.BlockChainImplBase blockChainService;

    @Captor
    private ArgumentCaptor<Proto.Ping> pingRequestCaptor;

    @Captor
    private ArgumentCaptor<Proto.TargetPeer> findPeersTargetCaptor;

    private GRpcPeerHandler peerHandler;

    private BranchId yggdrash;

    @Before
    public void setUp() {
        yggdrash = TestConstants.yggdrash();
        peerHandler = new GRpcPeerHandler(grpcServerRule.getChannel(), TARGET);
        grpcServerRule.getServiceRegistry().addService(peerService);
        grpcServerRule.getServiceRegistry().addService(blockChainService);
        assertEquals(TARGET, peerHandler.getPeer());
    }

    @Test
    public void ping() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(peerService).ping(pingRequestCaptor.capture(), any());

        Peer owner = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
        String ping = "Ping";
        peerHandler.ping(yggdrash, owner, ping);

        verify(peerService).ping(pingRequestCaptor.capture(), any());

        assertEquals(ping, pingRequestCaptor.getValue().getPing());

        ByteString branchId = pingRequestCaptor.getValue().getBranch();
        assertEquals(yggdrash, BranchId.of(branchId.toByteArray()));
    }

    @Test
    public void findPeers() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(peerService).findPeers(findPeersTargetCaptor.capture(), any());

        peerHandler.findPeers(yggdrash, TARGET);

        verify(peerService).findPeers(findPeersTargetCaptor.capture(), any());

        assertEquals("127.0.0.1", findPeersTargetCaptor.getValue().getIp());
        assertEquals(32918, findPeersTargetCaptor.getValue().getPort());
    }
}
