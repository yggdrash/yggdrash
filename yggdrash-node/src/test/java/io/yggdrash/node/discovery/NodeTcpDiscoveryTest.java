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

import io.yggdrash.TestConstants;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.node.TcpNodeTest;
import io.yggdrash.node.TestNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class NodeTcpDiscoveryTest extends TcpNodeTest {

    @Test
    public void test() {
        TestConstants.SlowTest.apply();

        // act
        bootstrapNodes(50);

        // assert
        for (TestNode node : nodeList) {
            nodeList.forEach(this::refreshAndHealthCheck);
            assertThat(node.getActivePeerCount()).isGreaterThanOrEqualTo(KademliaOptions.BUCKET_SIZE);
        }
    }
}
