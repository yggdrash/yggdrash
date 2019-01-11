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

package io.yggdrash.core.blockchain;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockHuskTest {

    private static final Logger log = LoggerFactory.getLogger(BlockHuskTest.class);

    private BlockHusk block;

    @Before
    public void setUp() {
        this.block = BlockChainTestUtils.genesisBlock();
    }

    @Test
    public void blockTest() {
        assert block.getInstance() != null;
        assert block.getIndex() == 0;
        assert block.verify();
    }

    @Test
    public void blockCloneTest() {
        BlockHusk cloned = new BlockHusk(block.getInstance());
        assert cloned.hashCode() == block.hashCode();
        assert cloned.compareTo(block) == 0;
    }

    @Test
    public void blockAddressTest() {
        assertThat(block.getAddress().toString())
                .isEqualTo("2b8d3ec39e8b8d86a6fcdf5f5fe375f30a6e6c06");
    }

    @Test
    public void deserializeTransactionFromProtoTest() {
        Proto.Block protoBlock = block.getInstance();
        BlockHusk deserializeBlock = new BlockHusk(protoBlock);
        assert block.getHash().equals(deserializeBlock.getHash());
    }

    @Test
    public void testToJsonObject() {
        //todo: modify to checking jsonObject when the block data format change to JsonObject.
        log.debug(block.toJsonObject().toString());
    }

    @Test
    public void constuctorTest() {
        BlockHusk block2 = new BlockHusk(new Block(block.toJsonObject()));
        assertThat(block2.verify()).isTrue();
        assertThat(block.toJsonObject().toString()).isEqualTo(block2.toJsonObject().toString());
    }


}
