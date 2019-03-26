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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.util.Utils;
import io.yggdrash.node.TcpNodeTesting;
import io.yggdrash.node.TestNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class BroadcastTxToValidatorTest extends TcpNodeTesting {

    @Test
    public void test() {
        // arrange
        TestNode deliveryNode = TestNode.createDeliveryNode(factory, SEED_PORT + 1);
        createAndStartServer(deliveryNode);
        deliveryNode.bootstrapping();
        TestNode validatorNode = createAndStartNode(32911, true);

        // act
        deliveryNode.getDefaultBranch().addTransaction(BlockChainTestUtils.createTransferTxHusk());
        Utils.sleep(500);

        // assert
        assertThat(validatorNode.countUnconfirmedTx()).isEqualTo(deliveryNode.countUnconfirmedTx());
        validatorNode.shutdown();
    }
}
