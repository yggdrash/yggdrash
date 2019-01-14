/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.node;

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.node.service.GRpcNodeServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GRpcYggdrashNodeTest {

    @Mock
    private NodeProperties nodePropertiesMock;

    @Mock
    private BranchGroup branchGroupMock;

    @Mock
    public NodeStatus nodeStatusMock;

    private GRpcYggdrashNode yggdrashNode;

    @Before
    public void setUp() {
        yggdrashNode = new GRpcYggdrashNode(nodePropertiesMock, nodeStatusMock, branchGroupMock);
        yggdrashNode.setNodeServer(new GRpcNodeServer());
    }

    @Test
    public void stop() {
        yggdrashNode.stop();
    }
}
