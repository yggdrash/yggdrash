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
import io.yggdrash.proto.Pong;
import org.junit.Rule;
import org.junit.Test;

public class NodeSyncServerTest {
    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Test
    public void pingPong() {
        grpcServerRule.getServiceRegistry().addService(new NodeSyncServer.PingPongImpl());

        PingPongGrpc.PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub
                (grpcServerRule.getChannel());
        String ping = "ping";

        Pong pong = blockingStub.play(Ping.newBuilder().setPing(ping).build());
        System.out.println(pong);
    }
}
