/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.Peer;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StoreBuilderTest {
    private static final BranchId BRANCH_ID = BranchId.NULL;
    private StoreBuilder builder;

    @Before
    public void setUp() {
        builder = new StoreBuilder(new DefaultConfig());
    }


    @Test
    public void shouldBeBuiltMetaStore() {
        BlockHusk block = BlockChainTestUtils.genesisBlock();
        StoreBuilder builder = new StoreBuilder(new DefaultConfig());
        BranchStore store = builder.buildMetaStore(BRANCH_ID);
        store.setBestBlock(block);

        assertThat(store.getBestBlockHash()).isEqualTo(block.getHash());
    }

    @Test
    public void buildBlockStore() {
        BlockHusk block = BlockChainTestUtils.genesisBlock();
        BlockStore store = builder.buildBlockStore(BRANCH_ID);
        store.put(block.getHash(), block);
        assertThat(store.contains(block.getHash())).isTrue();
        assertThat(store.get(block.getHash())).isEqualTo(block);
    }

    @Test
    public void buildTxStore() {
        TransactionHusk tx = BlockChainTestUtils.createTransferTxHusk();
        TransactionStore store = builder.buildTxStore(BRANCH_ID);
        store.put(tx.getHash(), tx);
        assertThat(store.contains(tx.getHash())).isTrue();
        assertThat(store.get(tx.getHash())).isEqualTo(tx);
    }

    @Test
    public void buildPeerStore() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        PeerStore store = builder.buildPeerStore(TestConstants.yggdrash());
        store.put(peer.getPeerId(), peer);
        assertThat(store.contains(peer.getPeerId())).isTrue();
        assertThat(store.get(peer.getPeerId())).isEqualTo(peer);
    }
}
