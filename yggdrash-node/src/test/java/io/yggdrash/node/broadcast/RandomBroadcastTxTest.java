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
import io.yggdrash.node.TestNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Consumer;

public class RandomBroadcastTxTest extends RandomBroadcastTesting {

    @Test
    public void test() {
        TestConstants.SlowTest.apply();

        // arrange
        rootLogger.setLevel(Level.ERROR);
        final int MAX_NODE_COUNT = 50;
        final int TX_COUNT = 100;

        bootstrapNodes(MAX_NODE_COUNT, true);
        nodeList.forEach(this::refreshAndHealthCheck);

        // act
        Consumer<TestNode> consumer = (n) ->
                n.getBranchGroup().addTransaction(BlockChainTestUtils.createTransferTxHusk());
        broadcastByRandomNode(TX_COUNT, consumer);

        // assert
        for (TestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            node.logDebugging();
            node.shutdown();
            Assert.assertEquals(TX_COUNT, node.getBranchGroup().getUnconfirmedTxs(TestConstants.yggdrash()).size());
        }
    }
}
