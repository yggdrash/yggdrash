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
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.node.AbstractNodeTesting;
import io.yggdrash.node.TestNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SimpleTxBroadcastTest extends AbstractNodeTesting {

    @Test
    @Ignore
    public void test() {
        // TODO 기본 브랜치 로딩을 중지하고, 테스트 계정에 프론티어 등록하여 금액을 추가한 후에 아래의 테스트를 진행 해야 합니다.
        TestConstants.SlowTest.apply();
        broadcastNetworkTest(20); // 4s;
        //broadcastNetworkTest(100); // (inProcess=39s, tcp=80s);
        //broadcastNetworkTest(300); // 6m
    }

    private void broadcastNetworkTest(int nodeCount) {
        // arrange
        rootLogger.setLevel(Level.INFO);

        bootstrapNodes(nodeCount, true); // 100 nodes  (inProcess=2ms, tcp=8s)
        nodeList.forEach(this::refreshAndHealthCheck); // 100 nodes  (tcp=2s)

        // act
        for (int i = 1; i < nodeCount; i++) {
            // broadcast 100 nodes (inProcess=300ms, tcp=700ms)
            Transaction tx = BlockChainTestUtils.createTransferTx();
            nodeList.get(i).getBranchGroup().addTransaction(tx);
            log.info("broadcast finish={}", i);
        }

        Utils.sleep(1000);

        // assert
        for (TestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            node.shutdown();
            Assert.assertEquals(nodeCount - 1,
                    node.getBranchGroup().getUnconfirmedTxs(TestConstants.yggdrash()).size());
        }
    }
}
