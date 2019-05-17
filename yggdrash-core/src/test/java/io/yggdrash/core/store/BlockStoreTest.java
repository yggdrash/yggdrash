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

package io.yggdrash.core.store;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.PbftProto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockStoreTest {
    private ConsensusBlockStore<PbftProto.PbftBlock> blockStore;

    @Before
    public void setUp() {
        LevelDbDataSource ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-store-test");
        blockStore = new PbftBlockStoreMock(ds);
    }

    @After
    public void tearDown() {
        blockStore.close();
        StoreTestUtils.clearTestDb();
    }

    @Test
    public void shouldBeGotBlock() {
        // arrange
        ConsensusBlock<PbftProto.PbftBlock> block = BlockChainTestUtils.genesisBlock();
        // act
        blockStore.put(block.getHash(), block);
        // assert
        assertThat(blockStore.contains(block.getHash())).isTrue();
        ConsensusBlock<PbftProto.PbftBlock> foundBlock = blockStore.get(block.getHash());
        assertThat(foundBlock).isEqualTo(block);
        assertThat(blockStore.size()).isEqualTo(1L);
    }

    @Test
    public void shouldBeGotBlockByIndex() {
        // arrange
        ConsensusBlock<PbftProto.PbftBlock> block = BlockChainTestUtils.genesisBlock();
        // act
        blockStore.addBlock(block);
        // assert
        assertThat(blockStore.contains(block.getHash())).isTrue();
        ConsensusBlock<PbftProto.PbftBlock> foundBlock = blockStore.getBlockByIndex(block.getIndex());
        assertThat(foundBlock).isEqualTo(block);
        assertThat(blockStore.size()).isEqualTo(1L);
    }
}
