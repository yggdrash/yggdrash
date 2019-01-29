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
import io.yggdrash.core.net.Peer;
import io.yggdrash.node.service.DiscoveryService;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GRpcInProcessYggdrashNodeTest extends GRpcYggdrashNodeTest {

    @Override
    protected ManagedChannel createChannel(Peer peer) {
        return InProcessChannelBuilder.forName(peer.getYnodeUri()).directExecutor().build();
    }

    @Override
    protected Server createServer(GRpcTestNode node) {
        String ynodeUri = node.peerTable.getOwner().getYnodeUri();
        Server server = InProcessServerBuilder.forName(ynodeUri).directExecutor().addService(
                new DiscoveryService(node.consumer)).build();
        gRpcCleanup.register(server);
        try {
            return server.start();
        } catch (Exception e) {
            return null;
        }
    }
}
