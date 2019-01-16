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

package io.yggdrash.node.grpc;

import io.yggdrash.TestConstants.SlowTest;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.net.KademliaDiscoveryMock;
import io.yggdrash.core.net.Node;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.GRpcPeerHandlerFactory;
import io.yggdrash.node.service.GRpcPeerListener;
import io.yggdrash.node.service.PeerService;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class GRpcYggdrashNodeTest extends SlowTest {
    private static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";

    @Test
    public void test() {
        NodeMock node1 = new NodeMock(32918);
        node1.bootstrapping();
        NodeMock node2 = new NodeMock(32919);
        node2.bootstrapping();
        NodeMock node3 = new NodeMock(32920);
        node3.bootstrapping();
        Utils.sleep(100);
        assert node1.getPeerGroup().getActivePeerList().size() == 2;
        assert node2.getPeerGroup().getActivePeerList().size() == 1;
        assert node3.getPeerGroup().getActivePeerList().size() == 1;
    }

    private class NodeMock extends Node {

        NodeMock(int port) {
            Peer owner = Peer.valueOf(NODE_URI_PREFIX + port);
            this.discovery = new KademliaDiscoveryMock(owner);

            PeerGroup peerGroup = discovery.getPeerGroup();
            List<String> seedList = Collections.singletonList("ynode://75bff16c@127.0.0.1:32918");
            peerGroup.setSeedPeerList(seedList);
            setServer(peerGroup);

            peerListener.start("", port);
        }

        void setServer(PeerGroup peerGroup) {
            peerGroup.setPeerHandlerFactory(new GRpcPeerHandlerFactory());
            GRpcPeerListener peerListener = new GRpcPeerListener();
            peerListener.addService(new PeerService(peerGroup));
            this.peerListener = peerListener;
        }

        PeerGroup getPeerGroup() {
            return discovery.getPeerGroup();
        }
    }
}
