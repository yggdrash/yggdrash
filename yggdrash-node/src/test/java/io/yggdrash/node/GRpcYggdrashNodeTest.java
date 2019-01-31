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
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.node.service.DiscoveryService;
import io.yggdrash.node.springboot.grpc.GrpcServerBuilderConfigurer;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class GRpcYggdrashNodeTest extends TestConstants.SlowTest {

    protected static final int SEED_PORT = 32918;

    private final AbstractApplicationContext context = new GenericApplicationContext();

    private PeerHandlerFactory factory;

    private List<GRpcTestNode> nodeList;

    @Rule
    public final GrpcCleanupRule gRpcCleanup = new GrpcCleanupRule();

    @Before
    public void setUp() {
        context.refresh();
        nodeList = new ArrayList<>();
        setPeerHandlerFactory();
    }

    @Test
    public void testDiscoveryLarge() {
        // act
        for (int i = SEED_PORT; i < SEED_PORT + 100; i++) {
            testNode(i).selfRefreshAndHealthCheck();
        }

        // log debugging
        Utils.sleep(500);
        for (GRpcTestNode node : nodeList) {
            node.logDebugging();
        }
    }

    @Test
    public void testDiscoverySmall() {
        // act
        testNode(SEED_PORT).selfRefreshAndHealthCheck();
        testNode(SEED_PORT + 1).selfRefreshAndHealthCheck();
        testNode(SEED_PORT + 2).selfRefreshAndHealthCheck();

        // assert
        Utils.sleep(100);
        assertThat(nodeList.get(0).getActivePeerCount()).isEqualTo(0);
        assertThat(nodeList.get(1).getActivePeerCount()).isEqualTo(1);
        assertThat(nodeList.get(2).getActivePeerCount()).isEqualTo(2);
    }

    private void setPeerHandlerFactory() {
        this.factory = peer -> {
            ManagedChannel managedChannel = createChannel(peer);
            gRpcCleanup.register(managedChannel);
            return new GRpcPeerHandler(managedChannel, peer);
        };
    }

    protected ManagedChannel createChannel(Peer peer) {
        return ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build();
    }

    protected GRpcTestNode testNode(int port) {
        GRpcTestNode node = new GRpcTestNode(factory, port);
        nodeList.add(node);
        Server server = createAndStartServer(node);
        gRpcCleanup.register(server);
        return node;
    }

    protected Server createAndStartServer(GRpcTestNode node) {
        GrpcServerBuilderConfigurer configurer = builder ->
                builder.addService(new DiscoveryService(node.consumer));

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
