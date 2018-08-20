/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private BlockHusk block;

    @Before
    public void setUp() {
        this.block = TestUtils.createGenesisBlockHusk();
    }

    @Test
    public void blockTest() {
        assert block.getInstance() != null;
        assert block.getIndex() == 0;
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

}
