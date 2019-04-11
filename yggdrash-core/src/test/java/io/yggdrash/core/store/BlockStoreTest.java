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
import io.yggdrash.core.consensus.Block;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

public class BlockStoreTest {

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

    @Test
    public void shouldBeGotBlock() {
        LevelDbDataSource ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-store-test");
        BlockHuskStore blockStore = new BlockHuskStore(ds);
        Block blockHuskFixture = BlockChainTestUtils.genesisBlock();
        blockStore.put(blockHuskFixture.getHash(), blockHuskFixture);
        Block foundBlockHusk = blockStore.get(blockHuskFixture.getHash());
        Assertions.assertThat(foundBlockHusk).isEqualTo(blockHuskFixture);
        Assertions.assertThat(blockStore.get(foundBlockHusk.getHash())).isEqualTo(foundBlockHusk);
    }
}
