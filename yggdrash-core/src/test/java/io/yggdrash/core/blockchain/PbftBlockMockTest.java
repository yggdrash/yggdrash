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

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PbftBlockMockTest {

    private ConsensusBlock genesisBlock;

    @Before
    public void setUp() {
        this.genesisBlock = BlockChainTestUtils.genesisBlock();
    }

    @Test
    public void genesisBlockTest() {
        assertThat(genesisBlock.getInstance()).isNotNull();
        assertThat(genesisBlock.getIndex()).isEqualTo(0);
    }

    @Test
    public void blockCloneTest() {
        PbftBlockMock cloned = new PbftBlockMock(genesisBlock.toBinary());
        assertThat(cloned.hashCode()).isEqualTo(genesisBlock.hashCode());
        assertThat(cloned).isEqualTo(genesisBlock);
    }

    @Test
    public void testToJsonObjectFromProtoObject() {
        JsonObject jsonObject = genesisBlock.toJsonObjectByProto();
        String jsonString = jsonObject.toString();
        assertThat(jsonObject).isNotNull();
        JsonObject jsonObjectBlock = jsonObject.getAsJsonObject("block").getAsJsonObject();
        assertThat(jsonObjectBlock.getAsJsonObject("header").get("index").getAsString()).isEqualTo("0");
        assertThat(jsonString).contains(genesisBlock.getHash().toString());
    }
}
