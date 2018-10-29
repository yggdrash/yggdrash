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

import io.yggdrash.TestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Paths;

public class MetaStoreTest {
    private MetaStore ms;

    @After
    public void tearDown() {
        FileUtil.recursiveDelete(Paths.get(TestUtils.YGG_HOME));
    }

    @Test
    public void shouldBeLoaded() {
        ms = new MetaStore(new LevelDbDataSource(getPath(), "meta"));
        BlockHusk blockHusk = new BlockHusk(TestUtils.getBlockFixture());
        ms.put(MetaStore.MetaInfo.BEST_BLOCK, blockHusk.getHash());
        Sha3Hash sha3Hash = ms.get(MetaStore.MetaInfo.BEST_BLOCK);
        Assertions.assertThat(sha3Hash).isEqualTo(blockHusk.getHash());

        ms.close();
        ms = new MetaStore(new LevelDbDataSource(getPath(), "meta"));
        Sha3Hash sha3HashAgain = ms.get(MetaStore.MetaInfo.BEST_BLOCK);
        Assertions.assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        ms = new MetaStore(new LevelDbDataSource(getPath(), "meta"));
        BlockHusk blockHusk = new BlockHusk(TestUtils.getBlockFixture());
        ms.put(MetaStore.MetaInfo.BEST_BLOCK, blockHusk.getHash());
        Sha3Hash sha3Hash = ms.get(MetaStore.MetaInfo.BEST_BLOCK);
        Assertions.assertThat(sha3Hash).isEqualTo(blockHusk.getHash());
    }

    private String getPath() {
        return Paths.get(TestUtils.YGG_HOME, "store").toString();
    }
}
