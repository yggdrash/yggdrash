/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerHandlerMock;
import io.yggdrash.core.net.PeerTableGroup;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import org.junit.Before;
import org.junit.Test;

public class PeerTaskTest {
    private final PeerTask peerTask = new PeerTask();
    private PeerTableGroup peerTableGroup;

    @Before
    public void setUp() {
        peerTableGroup = PeerTestUtils.createTableGroup();
        peerTableGroup.createTable(TestConstants.STEM);
        PeerHandlerGroup peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);

        peerTask.setPeerTableGroup(peerTableGroup);
        peerTask.setPeerHandlerGroup(peerHandlerGroup);
        peerTask.setNodeStatus(NodeStatusMock.mock);
    }

    @Test
    public void healthCheckTest() {
        peerTask.healthCheck();
    }

    @Test
    public void refreshTest() {
        assert peerTableGroup.getPeerTable(TestConstants.STEM).getBucketsCount() == 0;
        peerTask.refresh(); // seed added in selfRefresh
        assert peerTableGroup.getPeerTable(TestConstants.STEM).getBucketsCount() == 1;
    }
}