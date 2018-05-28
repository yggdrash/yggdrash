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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeSyncClient {
    public static final Logger log = LoggerFactory.getLogger(NodeSyncClient.class);

    private final ManagedChannel channel;
    private final PingPongGrpc.PingPongBlockingStub blockingStub;

    public NodeSyncClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    NodeSyncClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PingPongGrpc.newBlockingStub(channel);
    }

    public void ping(String message) {
        Ping request = Ping.newBuilder().setPing(message).build();
        Pong pong = blockingStub.play(request);
        log.debug(pong.getPong());
    }
}
