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

package io.yggdrash.node;

import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.net.NodeServer;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;

import java.io.IOException;

/**
 * The type Node sync demo server.
 */
public class NodeDemoServer {
    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        String host = "localhost";
        int port = 32918;
        NodeServer server = createNodeServer(host, port);
        server.start(host, port);
        server.blockUntilShutdown();
    }

    private static NodeServer createNodeServer(String host, int port) {
        GRpcNodeServer server = new GRpcNodeServer();
        Peer owner = Peer.valueOf("75bff16c", host, port);
        server.setPeerGroup(new PeerGroup(owner, 25));
        server.setBranchGroup(new BranchGroup());
        server.setWallet(TestConstants.wallet());
        server.setNodeStatus(new NodeStatus() {
            @Override
            public boolean isUpStatus() {
                return false;
            }

            @Override
            public void up() {

            }

            @Override
            public void sync() {

            }
        });
        return server;
    }
}
