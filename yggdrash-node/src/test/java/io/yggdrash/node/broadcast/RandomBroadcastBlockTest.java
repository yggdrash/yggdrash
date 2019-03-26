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
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.node.TestNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Consumer;

public class RandomBroadcastBlockTest extends RandomBroadcastTesting {

    @Test
    public void test() {
        TestConstants.SlowTest.apply();

        // arrange
        rootLogger.setLevel(Level.ERROR);
        final int maxNodeCount = 50;
        final int blockCount = 100;

        bootstrapNodes(maxNodeCount, true);
        nodeList.forEach(this::refreshAndHealthCheck);

        // act
        Consumer<TestNode> consumer = (n) -> {
            n.generateBlock();
            Utils.sleep(100);
        };
        broadcastByRandomNode(blockCount, consumer);

        // assert
        for (TestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            node.logDebugging();
            node.shutdown();
            Assert.assertEquals(blockCount, node.getDefaultBranch().getLastIndex());
        }
    }
}
