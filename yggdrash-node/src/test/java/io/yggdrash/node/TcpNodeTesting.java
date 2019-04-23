/*
 * Copyright 2019 Akashic Foundation
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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.springboot.grpc.GrpcServerBuilderConfigurer;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class TcpNodeTesting extends AbstractNodeTesting {

    private final AbstractApplicationContext context = new GenericApplicationContext();

    @Override
    public void setUp() {
        super.setUp();
        context.refresh();
    }

    @Override
    protected ManagedChannel createChannel(Peer peer) {
        return ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext().build();
    }

    @Override
    protected void createAndStartServer(TestNode node) {
        GrpcServerBuilderConfigurer configurer = builder -> addService(node, builder);

        GrpcServerRunner runner = new GrpcServerRunner(configurer, ServerBuilder.forPort(node.port));
        runner.setApplicationContext(context);
        try {
            runner.run();
            node.server = runner.getServer();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
