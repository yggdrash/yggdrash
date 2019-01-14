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
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

public class MetaStoreTest {
    private MetaStore ms;
    private static final BranchId BRANCH_ID = BranchId.NULL;

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

    @Test
    public void shouldBeLoaded() {
        ms = createMetaStore();
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setBestBlockHash(blockHusk.getHash());

        Sha3Hash sha3Hash = ms.getBestBlockHash();
        Assertions.assertThat(sha3Hash).isEqualTo(blockHusk.getHash());

        ms.close();
        ms = createMetaStore();
        Sha3Hash sha3HashAgain = ms.getBestBlockHash();
        Assertions.assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        ms = createMetaStore();
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setBestBlock(blockHusk);
        Long bestBlock = ms.getBestBlock();

        Assertions.assertThat(bestBlock).isEqualTo(blockHusk.getIndex());
    }

    private MetaStore createMetaStore() {
        LevelDbDataSource ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "meta");
        return new MetaStore(ds);
    }
}
