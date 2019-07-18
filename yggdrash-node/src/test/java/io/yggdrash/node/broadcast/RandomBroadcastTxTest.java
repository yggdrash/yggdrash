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
import org.junit.Ignore;
import org.junit.Test;

import java.util.function.Consumer;

public class RandomBroadcastTxTest extends RandomBroadcastTesting {

    @Test
    @Ignore
    public void test() {
        // TODO 기본 브랜치 로딩을 중지하고, 테스트 계정에 프론티어 등록하여 금액을 추가한 후에 아래의 테스트를 진행 해야 합니다.
        TestConstants.SlowTest.apply();

        // arrange
        rootLogger.setLevel(Level.ERROR);
        final int maxNodeCount = 50;
        final int txCount = 100;

        bootstrapNodes(maxNodeCount, true);
        nodeList.forEach(this::refreshAndHealthCheck);

        // act
        Consumer<TestNode> consumer = (n) ->
                n.getBranchGroup().addTransaction(BlockChainTestUtils.createTransferTx());
        broadcastByRandomNode(txCount, consumer);

        // assert
        for (TestNode node : nodeList) {
            if (node.isSeed()) {
                continue;
            }
            node.logDebugging();
            node.shutdown();
            Assert.assertEquals(txCount, node.getBranchGroup().getUnconfirmedTxs(TestConstants.yggdrash()).size());
        }
    }
}
