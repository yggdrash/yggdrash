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

package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BlockChainBuilderTest {

    @Test
    public void buildBlockChainTest() {
        GenesisBlock genesis = TestUtils.genesis();
        BlockChain blockChain = BlockChainBuilder.Builder()
                .setStoreBuilder(new StoreBuilder(new DefaultConfig()))
                .addGenesis(genesis).build();
        assertEquals(blockChain.getGenesisBlock().getHash(), genesis.getBlock().getHash());
    }

    @Test
    public void buildProductionBlockChainTest() {

        BlockChain bc1 = TestUtils.createBlockChain(false);
        BlockChain bc2 = TestUtils.createBlockChain(true);

        assertEquals(bc1.getGenesisBlock().getHash(), bc2.getGenesisBlock().getHash());
    }
}