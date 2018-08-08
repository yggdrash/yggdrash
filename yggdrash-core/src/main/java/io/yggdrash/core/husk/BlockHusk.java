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

package io.yggdrash.core.husk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.proto.BlockChainProto;

public class BlockHusk implements ProtoHusk<BlockChainProto.Block> {
    private BlockChainProto.Block block;

    public BlockHusk(byte[] bytes) throws InvalidProtocolBufferException {
        this.block = BlockChainProto.Block.parseFrom(bytes);
    }

    public BlockHusk(BlockChainProto.Block block) {
        this.block = block;
    }

    @Override
    public byte[] getData() {
        return block.toByteArray();
    }

    @Override
    public BlockChainProto.Block getInstance() {
        return this.block;
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(block.getHeader().toByteArray());
    }
}
