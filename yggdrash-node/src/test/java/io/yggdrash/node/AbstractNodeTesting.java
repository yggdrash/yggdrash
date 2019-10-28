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
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.p2p.BlockChainHandlerFactory;
import io.yggdrash.core.p2p.DiscoveryHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.service.BlockServiceFactory;
import io.yggdrash.node.service.DiscoveryService;
import io.yggdrash.node.service.PeerHandlerProvider;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractNodeTesting {
    protected static final Logger log = LoggerFactory.getLogger(AbstractNodeTesting.class);
    protected static final ch.qos.logback.classic.Logger rootLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    protected BlockChainHandlerFactory factory;

    protected List<TestNode> nodeList;

    static {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.grpc.netty")).setLevel(Level.INFO);
    }

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Before
    public void setUp() {
        this.nodeList = Collections.synchronizedList(new ArrayList<>());
        this.factory = mockFactory();
    }

    private void setFactory() {
        if (factory == null) {
            setUp();
        }
    }

    protected TestNode createAndStartNode(int port, boolean enableBranch) {
        setFactory();
        return createAndStartNode(new TestNode(factory, port, enableBranch));
    }

    protected TestNode createAndStartNode(int port, boolean enableBranch, List<String> seedPeerList) {
        setFactory();
        TestNode nodeWithSeedPeers = new TestNode(factory, port, enableBranch, seedPeerList);
        return createAndStartNode(nodeWithSeedPeers);
    }

    protected TestNode createAndStartNode(
            int port, boolean enableBranch, List<String> seedPeerList, List<String> validatorList) {
        setFactory();
        TestNode proxyNode = new TestNode(factory, port, enableBranch, seedPeerList, validatorList);
        return createAndStartNode(proxyNode);
    }

    private TestNode createAndStartNode(TestNode node) {
        nodeList.add(node);
        createAndStartServer(node);
        grpcCleanup.register(node.server);
        return node;
    }

    protected ManagedChannel createChannel(Peer peer) {
        return InProcessChannelBuilder.forName(peer.getYnodeUri()).directExecutor().build();
    }

    protected void createAndStartServer(TestNode node) {
        String ynodeUri = node.getPeer().getYnodeUri();
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(ynodeUri).directExecutor();
        addService(node, serverBuilder);
        node.server = serverBuilder.build();
        try {
            node.server.start();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    void addService(TestNode node, ServerBuilder builder) {
        node.discoveryConsumer.setListener(node.getSyncManger());
        builder.addService(new DiscoveryService(node.discoveryConsumer, null, null));
        if (node.transactionService != null) {
            builder.addService(node.transactionService);
            for (BlockChain blockChain : node.getBranchGroup().getAllBranch()) {
                builder.addService(BlockServiceFactory.create(blockChain.getConsensus().getAlgorithm(),
                        node.getBranchGroup(), node.getSyncManger()));
            }
        }
    }

    protected void bootstrapNodes(int nodeCount) {
        bootstrapNodes(nodeCount, false);
    }

    protected void bootstrapNodes(int nodeCount, boolean enableBranch) {
        for (int i = PeerTestUtils.SEED_PORT; i < PeerTestUtils.SEED_PORT + nodeCount; i++) {
            TestNode node = createAndStartNode(i, enableBranch);
            node.bootstrapping();
        }
    }

    protected void refreshAndHealthCheck(TestNode node) {
        node.peerTask.refresh();
        node.peerTask.healthCheck();
    }

    protected void shutdownNode(TestNode node) {
        node.shutdown();
        log.info("Stop nodePort={}", node.port);
        nodeList.remove(node);
    }

    private BlockChainHandlerFactory mockFactory() {
        return (consensusAlgorithm, peer) -> {
            ManagedChannel managedChannel = createChannel(peer);
            grpcCleanup.register(managedChannel);

            switch (consensusAlgorithm) {
                case "pbft":
                    return new PeerHandlerProvider.PbftPeerHandler(managedChannel, peer);
                case "ebft":
                    return new PeerHandlerProvider.EbftPeerHandler(managedChannel, peer);
                default:
                    // BS Node only
                    return new DiscoveryHandler(managedChannel, peer);
            }
        };
    }

}
