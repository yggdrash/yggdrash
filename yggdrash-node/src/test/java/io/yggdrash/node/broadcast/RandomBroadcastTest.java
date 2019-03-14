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

import io.yggdrash.common.util.Utils;
import io.yggdrash.node.AbstractNodeTest;
import io.yggdrash.node.TestNode;

import java.util.Random;
import java.util.function.Consumer;

class RandomBroadcastTest extends AbstractNodeTest {

    void broadcastByRandomNode(int execute, Consumer<TestNode> consumer) {
        for (int i = 0; i < execute; i++) {
            TestNode node = getRandomNode();
            try {
                consumer.accept(node);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            // shutdown random node per every 10 action
            if (i % 10 == 0) {
                // wait for broadcast
                Utils.sleep(500);

                shutdownNode(node);

                // for update peer table
                nodeList.forEach(n -> n.peerTask.healthCheck());
            }

            log.info("broadcast count={}", i + 1);
        }
        Utils.sleep(1000); // wait for broadcast
    }

    private TestNode getRandomNode() {
        Random r = new Random();
        int nodeIdx = r.nextInt(nodeList.size() - 1);
        if (nodeIdx == 0 ) { // exclude seed node
            nodeIdx = 1;
        }
        return nodeList.get(nodeIdx);
    }
}
