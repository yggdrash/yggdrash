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
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.yggdrash.TestConstants;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandler;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.node.service.GRpcDiscoveryService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class GRpcInProcessYggdrashNodeTest extends TestConstants.SlowTest {

    private static final int SEED_PORT = 32918;

    private final PeerHandlerFactory factory = new GRpcInProcessPeerHandlerFactory();

    private List<GRpcTestNode> nodeList = new ArrayList<>();

    @Rule
    public final GrpcCleanupRule gRpcCleanup = new GrpcCleanupRule();

    @Test
    public void testDiscoveryLarge() {
        // act
        for (int i = SEED_PORT; i < SEED_PORT + 100; i++) {
            createAndStartNode(i);
        }

        // log debugging
        for (GRpcTestNode node : nodeList) {
            node.logDebugging();
        }
    }

    private class GRpcInProcessPeerHandlerFactory implements PeerHandlerFactory {
        @Override
        public PeerHandler create(Peer peer) {
            ManagedChannel inProcessChannel = InProcessChannelBuilder.forName(peer.getYnodeUri())
                    .directExecutor().build();
            gRpcCleanup.register(inProcessChannel);
            return new GRpcPeerHandler(inProcessChannel, peer);
        }
    }

    private void createAndStartNode(int port) {
        GRpcTestNode node = new GRpcTestNode(factory, port);
        nodeList.add(node);
        String ynodeUri = node.peerTable.getOwner().getYnodeUri();
        Server server = InProcessServerBuilder.forName(ynodeUri).directExecutor().addService(
                new GRpcDiscoveryService(node.consumer)).build();
        gRpcCleanup.register(server);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        node.bootstrapping();
    }
}
