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

package io.yggdrash.node.broadcast;

import ch.qos.logback.classic.Level;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.node.AbstractNodeTest;
import io.yggdrash.node.TestNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Random;

@RunWith(JUnit4.class)
public class RandomBroadcastTest extends AbstractNodeTest {
    private static final int MAX_NODE_COUNT = 50;
    private static final int TX_COUNT = 50;
    private Thread txGeneratorThread;

    @Test
    public void test() {
        TestConstants.SlowTest.apply();

        // arrange
        rootLogger.setLevel(Level.ERROR);

        bootstrapNodes(MAX_NODE_COUNT, true);
        nodeList.forEach(this::refreshAndHealthCheck);

        // act
        broadcastTxByRandomNode();

        stopRandomNode();

        Utils.sleep(2000); // wait for broadcast

        // assert
        for (TestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            node.logDebugging();
            node.shutdown();
            log.info("nodePort={} txCount={}", node.port,
                    node.getBranchGroup().getUnconfirmedTxs(TestConstants.yggdrash()).size());
            Assert.assertEquals(TX_COUNT, node.getBranchGroup().getUnconfirmedTxs(TestConstants.yggdrash()).size());
        }
    }

    private void broadcastTxByRandomNode() {
        txGeneratorThread = new Thread(() -> {
            try {
                for (int i = 0; i < TX_COUNT; i++) {
                    TransactionHusk tx = BlockChainTestUtils.createTransferTxHusk();
                    TestNode node = getRandomNode();
                    node.getBranchGroup().addTransaction(tx);
                    log.info("broadcast txCount={}", i + 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        txGeneratorThread.start();
    }

    private void stopRandomNode() {
        while (true) {
            if (!txGeneratorThread.isAlive()) {
                return;
            }
            TestNode node = getRandomNode();
            node.shutdown();
            nodeList.remove(node);
            log.info("Stop nodePort={}, txGeneratorThread isAlive={}", node.port, txGeneratorThread.isAlive());
            nodeList.forEach(n -> n.peerTask.healthCheck());
            Utils.sleep(1000);
        }
    }

    private TestNode getRandomNode() {
        Random r = new Random();
        int nodeIdx = r.nextInt(nodeList.size() - 1);
        if (nodeIdx == 0) { // exclude seed node
            nodeIdx = 1;
        }
        return nodeList.get(nodeIdx);
    }
}
