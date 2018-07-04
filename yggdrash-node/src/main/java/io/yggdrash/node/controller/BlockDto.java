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

package io.yggdrash.node.controller;

import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;

import java.io.IOException;

public class BlockDto {
    private long index;
    private String hash;
    private String previousHash;
    private long timestamp;
    private BlockBody body;

    public static BlockDto createBy(Block block) throws IOException {
        BlockDto blockDto = new BlockDto();
        blockDto.setIndex(block.getIndex());
        blockDto.setHash(block.getBlockHash());
        blockDto.setPreviousHash(block.getPrevBlockHash());
        blockDto.setTimestamp(block.getTimestamp());
        return blockDto;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public BlockBody getBody() {
        return body;
    }

    public void setBody(BlockBody body) {
        this.body = body;
    }
}
