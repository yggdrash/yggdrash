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
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.PeerHandlerMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainSyncManagerTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManagerTest.class);

    private final BlockChainHandler handler = PeerHandlerMock.dummy();
    private BlockChainSyncManager syncManager;
    private BlockChain blockChain;
    private BlockChainManager blockChainManager;

    @Before
    public void setUp() {
        log.debug("init");
        BlockChainSyncManagerMock bcsmm = new BlockChainSyncManagerMock();

        syncManager = bcsmm.getMock();
        blockChain = BlockChainTestUtils.createBlockChain(false);
        blockChainManager = blockChain.getBlockChainManager();
    }

    @Test
    public void syncBlock() {
        assertThat(blockChainManager.getLastIndex()).isEqualTo(0);

        syncManager.syncBlock(handler, blockChain);

        assertThat(blockChainManager.getLastIndex()).isEqualTo(33);
    }

    @Test
    public void syncTransaction() {
        syncManager.syncTransaction(handler, blockChain);
        assertThat(blockChainManager.countOfTxs()).isEqualTo(3);
    }

}