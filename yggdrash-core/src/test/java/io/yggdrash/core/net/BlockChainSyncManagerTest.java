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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerHandlerMock;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainSyncManagerTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private final PeerHandler handler = PeerHandlerMock.dummy(OWNER);
    private BlockChainSyncManager syncManager;
    private BlockChain blockChain;

    @Before
    public void setUp() {
        syncManager = BlockChainSyncManagerMock.mock;
        blockChain = BlockChainTestUtils.createBlockChain(false);
    }

    @Test
    public void syncBlock() {
        assertThat(blockChain.getLastIndex()).isEqualTo(0);

        syncManager.syncBlock(handler, blockChain);

        assertThat(blockChain.getLastIndex()).isEqualTo(33);
    }

    @Test
    public void syncTransaction() {
        syncManager.syncTransaction(handler, blockChain);
    }

}