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

import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.TestUtils;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.proto.BlockChainProto;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class BlockStoreTest {
    private BlockStore blockStore;

    @Before
    public void setUp() throws Exception {
        this.blockStore = new BlockStore();
    }

    @Test
    public void shouldBeGotBlock() throws InvalidProtocolBufferException {
        BlockHusk blockHuskFixture = getBlockHuskFixture();
        blockStore.put(blockHuskFixture.getHash(), blockHuskFixture);
        BlockHusk foundBlockHusk = blockStore.get(blockHuskFixture.getHash());
        Assertions.assertThat(foundBlockHusk).isEqualTo(blockHuskFixture);
    }

    @Test
    public void shouldBePutBlock() throws InvalidProtocolBufferException {
        BlockHusk blockHusk = getBlockHuskFixture();
        blockStore.put(blockHusk.getHash(), blockHusk);
    }

    private BlockHusk getBlockHuskFixture() {
        BlockChainProto.Block blockFixture = TestUtils.getBlockFixture();
        return new BlockHusk(blockFixture);
    }
}
