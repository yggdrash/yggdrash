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
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.KademliaOptions;
import io.yggdrash.node.GRpcTestNode;
import io.yggdrash.node.discovery.AbstractDiscoveryNodeTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NetworkBroadcastTest extends AbstractDiscoveryNodeTest {

    @Test
    public void broadcastNetworkTest() {
        // arrange
        //TestConstants.SlowTest.apply();
        rootLogger.setLevel(Level.INFO);
        KademliaOptions.MAX_STEPS = 2;

        TransactionHusk testTx = BlockChainTestUtils.createTransferTxHusk();

        bootstrapNodes(3, true);
        nodeList.forEach(node -> node.peerTask.refresh());
        nodeList.forEach(node -> node.peerTask.healthCheck());

        // act
        nodeList.get(1).getBranchGroup().addTransaction(testTx);
        for (GRpcTestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            assert node.getBranchGroup().getUnconfirmedTxs(testTx.getBranchId()).get(0).getHash().equals(testTx.getHash());
        }
    }
}
