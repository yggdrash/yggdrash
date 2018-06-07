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
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class NodeSyncClientTest {
    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    private final PingPongGrpc.PingPongImplBase serviceImpl =
            mock(PingPongGrpc.PingPongImplBase.class, delegatesTo(
                    new NodeSyncServer.PingPongImpl() {}));
    private NodeSyncClient client;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(serviceImpl);
        client = new NodeSyncClient(grpcServerRule.getChannel());
    }

    @Test
    public void messageDeliveredToServer() {
        ArgumentCaptor<Ping> requestCaptor = ArgumentCaptor.forClass(Ping.class);
        String ping = "Ping";

        client.ping(ping);

        verify(serviceImpl)
                .play(requestCaptor.capture(), Matchers.any());
        assertEquals(ping, requestCaptor.getValue().getPing());
    }
}
