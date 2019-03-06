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

import io.yggdrash.node.AbstractNodeTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NodeSyncTest extends AbstractNodeTest {

    @Test
    public void bootstrapBlockSyncTest() {
        // arrange
        // bs node
        createNode(SEED_PORT, false).bootstrapping();
        // bootstrap -> generate block
        int node1 = 1;
        bootstrapSyncNode(node1);
        generateBlock(node1, 4);
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getLastIndex(), 4);

        // act
        int node2 = 2;
        bootstrapSyncNode(node2);

        // assert
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getLastIndex(),
                nodeList.get(node2).getDefaultBranch().getLastIndex());
    }

    @Test
    public void catchUpBlockSyncTest() {
        ////// arrange //////
        // 1) bs node: bootstrap
        createNode(SEED_PORT, false).bootstrapping();
        // 2) node1: bootstrap and generate block
        int node1 = 1;
        bootstrapSyncNode(node1);
        generateBlock(node1, 1);
        // 3) node2: bootstrap
        int node2 = 2;
        bootstrapSyncNode(node2);
        nodeList.get(node2).blockChainConsumer.setListener(nodeList.get(node2).getSyncManger());
        // 4) assert
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getLastIndex(),
                nodeList.get(node2).getDefaultBranch().getLastIndex());
        // 5) node1: before healthCheck no routing(node1 -> node2) and generate block
        generateBlock(node1, 1);

        // act
        // node1: after healthCheck added routing(node1 -> node2) and generate block
        nodeList.get(node1).peerTask.healthCheck();
        generateBlock(node1, 1);

        nodeList.get(node1).peerTask.getPeerDialer().destroyAll();
        nodeList.get(node2).peerTask.getPeerDialer().destroyAll();

        // assert
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getLastIndex(),
                nodeList.get(node2).getDefaultBranch().getLastIndex());
    }

    private void bootstrapSyncNode(int nodeIdx) {
        createNode(SEED_PORT + nodeIdx, true).bootstrapping();
    }

    private void generateBlock(int nodeIdx, int count) {
        for (int i = 0; i < count; i++) {
            nodeList.get(nodeIdx).generateBlock();
        }
    }
}