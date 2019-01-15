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

import io.yggdrash.core.net.Peer;

/**
 * The type Node sync demo client.
 */
public class NodeSyncDemoClient {
    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        GRpcClientChannel client =
                new GRpcClientChannel(Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        client.ping("Ping",
                Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        client.broadcastTransaction(GRpcClientChannelTest.sampleTxs());
    }
}
