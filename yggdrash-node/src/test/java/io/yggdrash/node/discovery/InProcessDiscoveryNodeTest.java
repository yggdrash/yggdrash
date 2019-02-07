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

import ch.qos.logback.classic.Level;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.net.KademliaOptions;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.util.PeerTableCounter;
import io.yggdrash.node.GRpcTestNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(JUnit4.class)
public class InProcessDiscoveryNodeTest extends AbstractDiscoveryNodeTest {

    @Test
    public void healthCheckWithBestBlockTest() {
        // arrange
        GRpcTestNode bsNode = testNode(SEED_PORT);
        bsNode.bootstrapping();
        GRpcTestNode node1 = testNode(SEED_PORT + 1);
        node1.bootstrapping();
        // update bestBlock
        node1.peerHandlerGroup.chainedBlock(BlockChainTestUtils.genesisBlock());

        // act (피어의 BestBlock 정보가 BS노드의 PeerTable에 등록됨)
        node1.peerTask.healthCheck();

        // assert
        Peer peer = bsNode.peerTable.getClosestPeers(bsNode.peerTable.getOwner(), 1).get(0);
        assert peer.getBestBlocks().size() == 1;
    }

    @Test
    public void underDefaultBucketSizeNetworkTest() {
        TestConstants.SlowTest.apply();
        for (int i = 2; i < KademliaOptions.BUCKET_SIZE; i++) {
            testDiscoveryNetwork(i, 1, 5);
            nodeList.clear();
        }
    }

    @Test
    public void targetSizeNetworkTest() {
        TestConstants.SlowTest.apply();
        rootLogger.setLevel(Level.INFO);
        testDiscoveryNetwork(100, 2, 7); // 1.5s
        //testDiscoveryNetwork(500, 2, 16); // 2s, maxPeers=96, maxBuckets=11
        //testDiscoveryNetwork(500, 3, 6); // 3s, maxPeers=96, maxBuckets=11
        //testDiscoveryNetwork(1000, 2, 16); // 4.8s, maxPeers=113, maxBuckets=13
        //testDiscoveryNetwork(5000, 3, 16); // 24s, maxPeers=138, maxBuckets=16
        //testDiscoveryNetwork(10000, 4, 16); // 65s, maxPeers=151, maxBuckets=17
    }

    private void testDiscoveryNetwork(int nodeCount, int step, int limit) {
        // arrange
        KademliaOptions.MAX_STEPS = step;
        PeerTableCounter counter = new PeerTableCounter();

        // act
        bootstrapAndHealthCheck(nodeCount);
        nodeList.forEach(node -> node.peerTask.refresh());

        // assert
        int maxBucket = 0;
        int maxPeers = 0;
        Set<Peer> peerSet = new HashSet<>();
        for (GRpcTestNode node : nodeList) {

            List<Peer> peerList = node.peerTable.getClosestPeers(node.peerTable.getOwner(), limit);
            peerSet.addAll(peerList);

            int peers = counter.use(node.peerTable).totalPeerOfBucket();
            int buket = node.peerTable.getBucketsCount();

            if (peers > maxPeers) {
                maxPeers = peers;
            }
            if (buket > maxBucket) {
                maxBucket = buket;
            }
        }
        log.info("nodeCount={}, discoveredPeerSize={}, maxPeers={}, maxBuckets={}",
                nodeCount, peerSet.size(), maxPeers, maxBucket);
        assert peerSet.size() == nodeCount;

        if (nodeCount == 50) {
            assert maxPeers == 42;
            assert maxBucket == 7;
        }
    }
}
