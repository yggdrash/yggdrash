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
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.proto.Proto;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;

import java.nio.file.Paths;

public class BlockStoreTest {
    private BlockStore blockStore;

    @AfterClass
    public static void destroy() {
        FileUtil.recursiveDelete(Paths.get(TestUtils.YGG_HOME));
    }

    @Test
    public void shouldBeGotBlock() {
        blockStore = new BlockStore(
                new LevelDbDataSource(getPath(), "get-test"));
        BlockHusk blockHuskFixture = getBlockHuskFixture();
        blockStore.put(blockHuskFixture.getHash(), blockHuskFixture);
        BlockHusk foundBlockHusk = blockStore.get(blockHuskFixture.getHash());
        Assertions.assertThat(foundBlockHusk).isEqualTo(blockHuskFixture);
        Assertions.assertThat(blockStore.get(foundBlockHusk.getHash())).isEqualTo(foundBlockHusk);
    }

    @Test
    public void shouldBePutBlock() {
        blockStore = new BlockStore(
                new LevelDbDataSource(getPath(), "put-test"));
        BlockHusk blockHusk = getBlockHuskFixture();
        blockStore.put(blockHusk.getHash(), blockHusk);
    }

    private BlockHusk getBlockHuskFixture() {
        Proto.Block blockFixture = TestUtils.getBlockFixture();
        return new BlockHusk(blockFixture);
    }

    private String getPath() {
        return Paths.get(TestUtils.YGG_HOME, "store").toString();
    }
}
