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

import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.node.service.GRpcPeerListener;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GRpcYggdrashNodeTest extends TestConstants.SlowTest {

    private static final int SEED_PORT = 32918;

    private final PeerHandlerFactory factory = new GRpcPeerHandlerFactory();

    private List<GRpcTestNode> nodeList = new ArrayList<>();

    @Test
    public void testDiscoveryLarge() {
        // act
        for (int i = SEED_PORT; i < SEED_PORT + 100; i++) {
            createAndStartNode(i);
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
        createAndStartNode(SEED_PORT);
        createAndStartNode(SEED_PORT + 1);
        createAndStartNode(SEED_PORT + 2);

        // assert
        Utils.sleep(100);
        assertThat(nodeList.get(0).getActivePeerCount()).isEqualTo(0);
        assertThat(nodeList.get(1).getActivePeerCount()).isEqualTo(1);
        assertThat(nodeList.get(2).getActivePeerCount()).isEqualTo(2);
    }

    @After
    public void tearDown() {
        nodeList.forEach(GRpcTestNode::stop);
        nodeList.clear();
    }

    private void createAndStartNode(int port) {
        GRpcTestNode node = new GRpcTestNode(factory, port);
        nodeList.add(node);
        GRpcPeerListener peerListener = new GRpcPeerListener();
        peerListener.initConsumer(node.consumer, null);
        node.setPeerListener(peerListener);
        peerListener.start("", node.port);
        node.bootstrapping();
    }
}
