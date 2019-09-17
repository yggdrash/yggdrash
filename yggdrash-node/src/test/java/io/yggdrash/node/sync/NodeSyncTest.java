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

import io.yggdrash.PeerTestUtils;
import io.yggdrash.common.util.Utils;
import io.yggdrash.node.AbstractNodeTesting;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore // TODO: check tests
public class NodeSyncTest extends AbstractNodeTesting {

    @Test
    public void catchUpBlockSyncTest() {
        // arrange
        int node1 = 1;
        bootstrapBlockSyncTest();

        // act
        // node1 > before healthCheck no routing(node1 -> node2) and generate block
        generateBlock(node1, 1);
        // node1 > after healthCheck added routing(node1 -> node2) and generate block
        nodeList.get(node1).peerTask.healthCheck();
        generateBlock(node1, 1);

        Utils.sleep(500); // wait for broadcast

        // assert
        int node2 = 2;
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getBlockChainManager().getLastIndex(),
                nodeList.get(node2).getDefaultBranch().getBlockChainManager().getLastIndex());
    }

    private void bootstrapBlockSyncTest() {
        // arrange
        int node1 = 1;
        arrangeForSync(node1, 4);

        // act
        int node2 = 2;
        bootstrapNodesByIndex(node2);

        // assert
        Assert.assertEquals(nodeList.get(node1).getDefaultBranch().getBlockChainManager().getLastIndex(),
                nodeList.get(node2).getDefaultBranch().getBlockChainManager().getLastIndex());
    }

    private void arrangeForSync(int node1, int generateBlockCount) {
        // 1) bsNode > bootstrap
        int bsdNode = 0;
        bootstrapNodesByIndex(bsdNode);

        // 2) node1 > bootstrap
        bootstrapNodesByIndex(node1);

        // 3) bsNode > healthCheck: -> node1
        nodeList.get(bsdNode).peerTask.healthCheck();

        // 4) node1 > generate Block
        generateBlock(node1, generateBlockCount);
        Assert.assertEquals(generateBlockCount,
                nodeList.get(node1).getDefaultBranch().getBlockChainManager().getLastIndex());
    }

    private void bootstrapNodesByIndex(int nodeIdx) {
        createAndStartNode(PeerTestUtils.SEED_PORT + nodeIdx, true).bootstrapping();
    }

    private void generateBlock(int nodeIdx, int count) {
        for (int i = 0; i < count; i++) {
            nodeList.get(nodeIdx).generateBlock();
        }
    }
}