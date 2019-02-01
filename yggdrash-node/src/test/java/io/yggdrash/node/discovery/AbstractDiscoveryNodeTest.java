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

import ch.qos.logback.classic.Level;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.node.GRpcPeerHandler;
import io.yggdrash.node.GRpcTestNode;
import io.yggdrash.node.service.DiscoveryService;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AbstractDiscoveryNodeTest {
    static final Logger log = LoggerFactory.getLogger(AbstractDiscoveryNodeTest.class);
    static final ch.qos.logback.classic.Logger rootLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    static final int SEED_PORT = 32918;

    private PeerHandlerFactory factory;

    List<GRpcTestNode> nodeList;

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

    GRpcTestNode testNode(int port) {
        GRpcTestNode node = new GRpcTestNode(factory, port);
        nodeList.add(node);
        Server server = createAndStartServer(node);
        gRpcCleanup.register(server);
        return node;
    }

    protected ManagedChannel createChannel(Peer peer) {
        return InProcessChannelBuilder.forName(peer.getYnodeUri()).directExecutor().build();
    }

    protected Server createAndStartServer(GRpcTestNode node) {
        String ynodeUri = node.peerTable.getOwner().getYnodeUri();
        Server server = InProcessServerBuilder.forName(ynodeUri).directExecutor().addService(
                new DiscoveryService(node.consumer)).build();
        try {
            return server.start();
        } catch (Exception e) {
            log.debug(e.getMessage());
            return server;
        }
    }

    void bootstrapAndHealthCheck(int nodeCount) {
        for (int i = SEED_PORT; i < SEED_PORT + nodeCount; i++) {
            GRpcTestNode node = testNode(i);
            node.bootstrapping();
            node.peerTask.healthCheck();
        }
    }

    GRpcTestNode refreshAndhealthCheck(GRpcTestNode node) {
        node.peerTask.refresh();
        node.peerTask.healthCheck();
        return node;
    }
}
