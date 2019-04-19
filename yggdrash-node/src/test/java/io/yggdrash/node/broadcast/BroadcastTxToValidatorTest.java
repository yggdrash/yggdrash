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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BroadcastTxToValidatorTest extends TcpNodeTesting {

    @Test
    @Ignore
    public void test() {
        // arrange
        // validator
        TestNode validatorNode = createAndStartNode(32801, true);
        List<String> validatorList = Collections.singletonList(validatorNode.getPeer().getYnodeUri());
        // delivery
        TestNode deliveryNode = TestNode.createDeliveryNode(factory, validatorList);
        deliveryNode.bootstrapping();

        // act
        deliveryNode.getDefaultBranch().addTransaction(BlockChainTestUtils.createTransferTxHusk());
        Utils.sleep(1000);

        // assert
        //TODO
        //   Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException
        //   Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException
        assertThat(validatorNode.countUnconfirmedTx()).isEqualTo(deliveryNode.countUnconfirmedTx());
        validatorNode.shutdown();
    }
}
