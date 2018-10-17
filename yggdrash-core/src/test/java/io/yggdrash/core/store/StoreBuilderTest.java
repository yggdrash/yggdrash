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

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.net.Peer;
import org.junit.Before;
import org.junit.Test;

public class StoreBuilderTest {
    private static final BranchId BRANCH_ID = BranchId.stem();
    private StoreBuilder builder;

    @Before
    public void setUp() {
        builder = new StoreBuilder(false);
    }

    @Test
    public void buildBlockStore() {
        BlockHusk block = TestUtils.createGenesisBlockHusk();
        BlockStore store = builder.buildBlockStore(BRANCH_ID);
        store.put(block.getHash(), block);
        assert store.contains(block.getHash());
        assert store.get(block.getIndex()).equals(block);
        assert store.get(block.getHash()).equals(block);
    }

    @Test
    public void buildTxStore() {
        TransactionHusk tx = TestUtils.createTransferTxHusk();
        TransactionStore store = builder.buildTxStore(BRANCH_ID);
        store.put(tx.getHash(), tx);
        assert store.contains(tx.getHash());
        assert store.get(tx.getHash()).equals(tx);
    }

    @Test
    public void buildPeerStore() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        PeerStore store = builder.buildPeerStore(BRANCH_ID);
        store.put(peer.getPeerId(), peer);
        assert store.contains(peer.getPeerId());
        assert store.get(peer.getPeerId()).equals(peer);
    }
}