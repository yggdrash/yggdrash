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

package io.yggdrash.node.discovery;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.TestConstants;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.GRpcTestNode;
import io.yggdrash.node.service.BlockChainService;
import io.yggdrash.node.service.DiscoveryService;
import io.yggdrash.node.springboot.grpc.GrpcServerBuilderConfigurer;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TcpDiscoveryNodeTest extends AbstractDiscoveryNodeTest {

    private final AbstractApplicationContext context = new GenericApplicationContext();

    @Override
    public void setUp() {
        super.setUp();
        context.refresh();
    }

    @Test
    public void test() {
        TestConstants.SlowTest.apply();

        // act
        bootstrapNodes(50);

        // assert
        for (GRpcTestNode node : nodeList) {
            nodeList.forEach(this::refreshAndHealthCheck);
            assertThat(node.getActivePeerCount()).isGreaterThanOrEqualTo(KademliaOptions.BUCKET_SIZE);
        }
    }

    @Override
    protected ManagedChannel createChannel(Peer peer) {
        return ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build();
    }

    @Override
    protected Server createAndStartServer(GRpcTestNode node) {
        GrpcServerBuilderConfigurer configurer = builder -> {
            builder.addService(new DiscoveryService(node.discoveryConsumer));
            if (node.blockChainConsumer != null) {
                builder.addService(new BlockChainService(node.blockChainConsumer));
            }
        };

        GrpcServerRunner runner = new GrpcServerRunner(configurer,
                ServerBuilder.forPort(node.port));
        runner.setApplicationContext(context);
        try {
            runner.run();
            return runner.getServer();
        } catch (Exception e) {
            return null;
        }
    }
}
