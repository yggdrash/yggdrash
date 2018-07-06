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

import io.grpc.testing.GrpcServerRule;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class NodeSyncClientTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    private final PingPongGrpc.PingPongImplBase pingPongService =
            mock(PingPongGrpc.PingPongImplBase.class, delegatesTo(
                    new NodeSyncServer.PingPongImpl() {
                    }));

    private final BlockChainGrpc.BlockChainImplBase blockChainService =
            mock(BlockChainGrpc.BlockChainImplBase.class, delegatesTo(
                    new NodeSyncServer.BlockChainImpl() {
                    }));

    private NodeSyncClient client;

    @Before
    public void setUp() {
        client = new NodeSyncClient(grpcServerRule.getChannel());
    }

    @Test
    public void play() {
        grpcServerRule.getServiceRegistry().addService(pingPongService);
        ArgumentCaptor<Ping> requestCaptor = ArgumentCaptor.forClass(Ping.class);
        String ping = "Ping";

        client.ping(ping);

        verify(pingPongService).play(requestCaptor.capture(), any());

        assertEquals(ping, requestCaptor.getValue().getPing());
    }

    @Test
    public void broadcastTransaction() {
        grpcServerRule.getServiceRegistry().addService(blockChainService);

        client.broadcastTransaction(NodeTestData.transactions());

        verify(blockChainService).broadcastTransaction(any());
    }

    @Test
    public void broadcastBlock() {
        grpcServerRule.getServiceRegistry().addService(blockChainService);

        client.broadcastBlock(NodeTestData.blocks());

        verify(blockChainService).broadcastBlock(any());
    }

}
