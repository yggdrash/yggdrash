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

package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import org.junit.Before;
import org.junit.Test;

public class PeerTaskTest {
    private final PeerTask peerTask = new PeerTask();
    private KademliaPeerTable peerTable;
    private PeerHandlerGroup peerHandlerGroup;

    @Before
    public void setUp() {
        peerTask.setNodeStatus(NodeStatusMock.mock);
        peerTable = PeerTestUtils.createTable();
        peerTask.setPeerTable(peerTable);
        peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
        peerTask.setPeerHandlerGroup(peerHandlerGroup);
    }

    @Test
    public void healthCheckTest() {
        peerTask.healthCheck();
    }

    @Test
    public void revalidateTest() {
        peerTable.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        peerTable.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        peerTask.revalidate();
    }

    @Test
    public void refreshTest() {
        assert peerTable.getBucketsCount() == 0;
        peerTask.refresh(); // seed added in selfRefresh
        assert peerTable.getBucketsCount() == 1;
    }

    @Test
    public void copyNodeTest() {
        peerTask.copyNode();
        assert peerTable.getPeerStore().size() == 0;
    }

}