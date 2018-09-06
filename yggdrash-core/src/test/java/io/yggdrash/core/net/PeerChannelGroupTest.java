/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.mock.ChannelMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerChannelGroupTest {
    private static final Logger logger = LoggerFactory.getLogger(BlockStore.class);

    private static final int MAX_PEERS = 25;

    private PeerChannelGroup peerChannelGroup;
    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        this.tx = TestUtils.createTxHusk();
        this.block = TestUtils.createGenesisBlockHusk();
        this.peerChannelGroup = new PeerChannelGroup(MAX_PEERS);
        peerChannelGroup.setListener(peer -> logger.debug(peer.getYnodeUri() + " disconnected"));
        ChannelMock channel = new ChannelMock("ynode://75bff16c@localhost:9999");
        peerChannelGroup.newPeerChannel(channel);
    }

    @Test
    public void healthCheck() {
        peerChannelGroup.healthCheck();
        assert !peerChannelGroup.getActivePeerList().isEmpty();
    }

    @Test
    public void syncBlock() {
        peerChannelGroup.chainedBlock(block);
        assert !peerChannelGroup.syncBlock(0).isEmpty();
    }

    @Test
    public void syncTransaction() {
        peerChannelGroup.newTransaction(tx);
        assert !peerChannelGroup.syncTransaction().isEmpty();
    }

    @Test
    public void addActivePeer() {
        int testCount = MAX_PEERS + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 9000;
            ChannelMock channel = new ChannelMock("ynode://75bff16c@localhost:" + port);
            peerChannelGroup.newPeerChannel(channel);
        }
        assert MAX_PEERS == peerChannelGroup.getActivePeerList().size();
    }

    @Test
    public void broadcastPeerConnect() {
        assert !peerChannelGroup.broadcastPeerConnect("ynode://75bff16c@localhost:9999").isEmpty();
    }

    @Test
    public void broadcastPeerDisconnect() {
        assert !peerChannelGroup.getActivePeerList().isEmpty();
        peerChannelGroup.broadcastPeerDisconnect("ynode://75bff16c@localhost:9999");
        assert peerChannelGroup.getActivePeerList().isEmpty();
    }
}
