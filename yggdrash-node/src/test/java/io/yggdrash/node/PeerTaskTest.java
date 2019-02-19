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
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerMock;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.core.p2p.SimplePeerDialer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PeerTaskTest {
    private final PeerTask peerTask = new PeerTask();
    private PeerTableGroup peerTableGroup;
    private BranchId yggdrash;

    @Before
    public void setUp() {
        yggdrash = TestConstants.yggdrash();
        peerTableGroup = PeerTestUtils.createTableGroup();
        peerTableGroup.createTable(yggdrash);
        PeerDialer peerDialer = new SimplePeerDialer(PeerHandlerMock.factory);

        peerTask.setPeerTableGroup(peerTableGroup);
        peerTask.setPeerDialer(peerDialer);
        peerTask.setNodeStatus(NodeStatusMock.mock);
    }

    @Test
    public void healthCheckTest() {
        peerTask.healthCheck();
    }

    @Test
    public void refreshTest() {
        assertEquals(0, peerTableGroup.getPeerTable(yggdrash).getBucketsCount());
        peerTask.refresh(); // seed added in selfRefresh
        assertEquals(4, peerTableGroup.getPeerTable(yggdrash).getBucketsCount());
    }
}