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

import ch.qos.logback.classic.Level;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandlerFactory;
import io.yggdrash.node.service.BlockChainService;
import io.yggdrash.node.service.DiscoveryService;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AbstractNodeTest {
    protected static final Logger log = LoggerFactory.getLogger(AbstractNodeTest.class);
    protected static final ch.qos.logback.classic.Logger rootLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    protected static final int SEED_PORT = 32918;

    private PeerHandlerFactory factory;

    protected List<TestNode> nodeList;

    static {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.grpc.netty")).setLevel(Level.INFO);
    }

    @Rule
    public GrpcCleanupRule gRpcCleanup = new GrpcCleanupRule();

    @Before
    public void setUp() {
        nodeList = new ArrayList<>();
        setPeerHandlerFactory();
    }

    private void setPeerHandlerFactory() {
        this.factory = peer -> {
            ManagedChannel managedChannel = createChannel(peer);
            gRpcCleanup.register(managedChannel);
            return new GRpcPeerHandler(managedChannel, peer);
        };
    }

    protected TestNode createNode(int port, boolean enableBranch) {
        TestNode node = new TestNode(factory, port, enableBranch);
        nodeList.add(node);
        Server server = createAndStartServer(node);
        gRpcCleanup.register(server);
        return node;
    }

    protected ManagedChannel createChannel(Peer peer) {
        return InProcessChannelBuilder.forName(peer.getYnodeUri()).directExecutor().build();
    }

    protected Server createAndStartServer(TestNode node) {
        String ynodeUri = node.peerTableGroup.getOwner().getYnodeUri();
        InProcessServerBuilder builder = InProcessServerBuilder.forName(ynodeUri).directExecutor().addService(
                new DiscoveryService(node.discoveryConsumer));

        if (node.blockChainConsumer != null) {
            builder.addService(new BlockChainService(node.blockChainConsumer));
        }
        Server server = builder.build();
        try {
            return server.start();
        } catch (Exception e) {
            log.debug(e.getMessage());
            return server;
        }
    }

    protected void bootstrapNodes(int nodeCount) {
        bootstrapNodes(nodeCount, false);
    }

    protected void bootstrapNodes(int nodeCount, boolean enableBranch) {
        for (int i = SEED_PORT; i < SEED_PORT + nodeCount; i++) {
            TestNode node = createNode(i, enableBranch);
            node.bootstrapping();
        }
    }

    protected TestNode refreshAndHealthCheck(TestNode node) {
        node.peerTask.refresh();
        node.peerTask.healthCheck();
        return node;
    }
}
