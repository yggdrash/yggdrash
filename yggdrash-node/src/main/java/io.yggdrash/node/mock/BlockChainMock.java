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

package io.yggdrash.node.mock;

import io.yggdrash.core.Block;
import io.yggdrash.node.BlockChain;

import java.util.LinkedHashMap;

public class BlockChainMock implements BlockChain {
    private LinkedHashMap<String, Block> blocks = new LinkedHashMap<>();

    @Override
    public Block addBlock(Block nextBlock) {
        blocks.put(nextBlock.getBlockHash(), nextBlock);
        blocks.put(String.valueOf(nextBlock.getIndex()), nextBlock);
        return nextBlock;
    }

    @Override
    public Block getBlockByIndex(int index) {
        return blocks.get(String.valueOf(index));
    }

    @Override
    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }

    @Override
    public LinkedHashMap<byte[], Block> getBlocks() {
        return null;
    }
}
