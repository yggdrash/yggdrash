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

package io.yggdrash.node.sync;

import ch.qos.logback.classic.Level;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.node.TestNode;
import io.yggdrash.node.discovery.NodeTcpDiscoveryTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NodeSyncLimitTest extends NodeTcpDiscoveryTest {

    @Override
    @Ignore
    @Test
    public void test() {
        rootLogger.setLevel(Level.INFO);

        // arrange
        // Node1 : start -> generate tx -> generate block
        TestNode node1 = createAndStartNode(8888, true);
        int blockCount = 30;
        generateTxAndBlock(node1, blockCount, 500);
        assert node1.getDefaultBranch().getLastIndex() == blockCount;
        // Node2 : start -> add route node2 -> node1
        TestNode node2 = createAndStartNode(8889, true);
        node2.peerTableGroup.addPeer(TestConstants.yggdrash(), node1.peerTableGroup.getOwner());

        // act sync block
        node2.bootstrapping();

        // assert
        assert node1.getDefaultBranch().getLastIndex() == node2.getDefaultBranch().getLastIndex();
    }

    private void generateTxAndBlock(TestNode node, int blockCount, int txCount) {
        for (int i = 0; i < blockCount; i++) {
            for (int j = 0; j < txCount; j++) {
                node.getBranchGroup().addTransaction(BlockChainTestUtils.createTransferTxHusk());
            }
            node.generateBlock();
        }
    }
}