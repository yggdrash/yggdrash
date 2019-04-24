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

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.p2p.BlockChainDialer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NodeHealthIndicatorTest {
    private static final Status SYNC = new Status("SYNC", "Synchronizing..");

    @Mock
    private BranchGroup branchGroupMock;

    private NodeHealthIndicator nodeHealthIndicator;

    @Before
    public void setUp() {
        DefaultConfig defaultConfig = new DefaultConfig();
        PeerDialer peerDialer = new BlockChainDialer(PeerHandlerMock.factory);
        this.nodeHealthIndicator = new NodeHealthIndicator(defaultConfig, branchGroupMock,
                peerDialer);
    }

    @Test
    public void health() {
        Health health = nodeHealthIndicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("yggdrash", health.getDetails().get("name"));
        assertNotNull(health.getDetails().get("branches"));
        assertEquals(0, (int) health.getDetails().get("activePeers"));
    }

    @Test
    public void up() {
        assertEquals(Status.DOWN, nodeHealthIndicator.health().getStatus());
        nodeHealthIndicator.up();
        assertEquals(Status.UP, nodeHealthIndicator.health().getStatus());
    }

    @Test
    public void sync() {
        assertEquals(Status.DOWN, nodeHealthIndicator.health().getStatus());
        nodeHealthIndicator.sync();
        assertEquals(SYNC.getCode(), nodeHealthIndicator.health().getStatus().getCode());
    }
}
