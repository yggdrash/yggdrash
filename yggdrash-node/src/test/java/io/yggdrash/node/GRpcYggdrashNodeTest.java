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

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.net.Discovery;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.net.KademliaDiscovery;
import io.yggdrash.core.net.Node;
import io.yggdrash.core.net.PeerTable;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import io.yggdrash.node.service.GRpcPeerListener;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class GRpcYggdrashNodeTest extends TestConstants.SlowTest {
    protected static final Logger log = LoggerFactory.getLogger(GRpcYggdrashNodeTest.class);
    private static final int SEED_PORT = 32918;
    private List<GRpcTestNode> nodeList = new ArrayList<>();

    @After
    public void tearDown() {
        nodeList.forEach(GRpcTestNode::stop);
        nodeList.clear();
    }

    @Test
    public void testDiscoverySmall() {
        // act
        GRpcTestNode node1 = new GRpcTestNode(SEED_PORT).start();
        node1.bootstrapping();
        GRpcTestNode node2 = new GRpcTestNode(32919).start();
        node2.bootstrapping();
        GRpcTestNode node3 = new GRpcTestNode(32920).start();
        node3.bootstrapping();

        // assert
        Utils.sleep(100);
        assertThat(node1.getActivePeerCount()).isEqualTo(0);
        assertThat(node2.getActivePeerCount()).isEqualTo(1);
        assertThat(node3.getActivePeerCount()).isEqualTo(2);
    }

    @Test
    public void testDiscoveryLarge() {
        // act
        for (int i = SEED_PORT; i < SEED_PORT + 50; i++) {
            GRpcTestNode node = new GRpcTestNode(i).start();
            node.bootstrapping();
        }

        // log debugging
        Utils.sleep(500);
        for (GRpcTestNode node : nodeList) {
            log.info("{} => peerStore={}, peerBucket={}, active={}",
                    node.peerTable.getOwner(),
                    node.peerTable.getStoreCount(),
                    node.peerTable.getBucketsCount(),
                    node.getActivePeerCount());
        }
    }

    private class GRpcTestNode extends Node {
        private static final int MAX_PEERS = 25;
        private PeerTable peerTable;
        private int port;

        GRpcTestNode(int port) {
            this.peerHandlerGroup = new SimplePeerHandlerGroup(new GRpcPeerHandlerFactory());
            this.peerTable = PeerTestUtils.createPeerTable(port);
            this.port = port;

            setListener();
        }

        GRpcTestNode start() {
            log.debug("Start listener port={}", port);
            peerListener.start("", port);
            return this;
        }

        void setListener() {
            GRpcPeerListener peerListener = new GRpcPeerListener();
            DiscoveryConsumer consumer = new DiscoveryServiceConsumer(peerTable);
            peerListener.initConsumer(consumer, null);
            this.peerListener = peerListener;
        }

        void bootstrapping() {
            Discovery discovery = new KademliaDiscovery(peerTable);

            super.bootstrapping(discovery, MAX_PEERS);
            nodeList.add(this);
        }

        int getActivePeerCount() {
            return peerHandlerGroup.getActivePeerList().size();
        }
    }
}
