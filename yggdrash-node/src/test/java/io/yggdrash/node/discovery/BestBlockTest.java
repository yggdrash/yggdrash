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

package io.yggdrash.node.discovery;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.net.Peer;
import io.yggdrash.node.GRpcInProcessYggdrashNodeTest;
import io.yggdrash.node.GRpcTestNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BestBlockTest extends GRpcInProcessYggdrashNodeTest {
    private GRpcTestNode bsNode;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        bsNode = testNode(SEED_PORT).selfRefreshAndHealthCheck();
    }

    @Override
    public void testDiscoveryLarge() {
    }

    @Test
    @Override
    public void testDiscoverySmall() {
        // arrange
        GRpcTestNode normal = testNode(SEED_PORT + 1);
        // update bestBlock
        normal.peerHandlerGroup.chainedBlock(BlockChainTestUtils.genesisBlock());

        // act (피어의 BestBlock 정보가 BS노드의 PeerTable에 등록됨)
        normal.selfRefreshAndHealthCheck();

        // assert
        Peer peer = bsNode.peerTable.getClosestPeers(bsNode.peerTable.getOwner(), 1).get(0);
        assert peer.getBestBlocks().size() == 1;
    }
}
