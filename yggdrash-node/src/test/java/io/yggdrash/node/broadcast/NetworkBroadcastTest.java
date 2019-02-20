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
import io.yggdrash.core.p2p.KademliaOptions;
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
        rootLogger.setLevel(Level.INFO);
        KademliaOptions.MAX_STEPS = 2;

        bootstrapNodes(3, true);
        nodeList.forEach(this::refreshAndHealthCheck);

        // act
        TransactionHusk testTx = BlockChainTestUtils.createTransferTxHusk();
        nodeList.get(1).getBranchGroup().addTransaction(testTx);

        // assert
        for (GRpcTestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            assert node.getBranchGroup().getUnconfirmedTxs(testTx.getBranchId())
                    .get(0).getHash().equals(testTx.getHash());
        }
    }
}
