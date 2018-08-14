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
import io.yggdrash.proto.Proto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlockHusk implements ProtoHusk<Proto.BlockV2> {
    private Proto.BlockV2 block;

    public BlockHusk(byte[] bytes) throws InvalidProtocolBufferException {
        this.block = Proto.BlockV2.parseFrom(bytes);
    }

    public BlockHusk(Proto.BlockV2 block) {
        this.block = block;
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(block.getHeader().toByteArray());
    }

    public long getIndex() {
        return this.block.getHeader().getRawData().getIndex();
    }

    public List<TransactionHusk> getBody() {
        List<TransactionHusk> result = new ArrayList<>();
        for (Proto.TransactionV2 tx : block.getBodyList()) {
            result.add(new TransactionHusk(tx));
        }
        return result;
    }

    @Override
    public byte[] getData() {
        return block.toByteArray();
    }

    @Override
    public Proto.BlockV2 getInstance() {
        return this.block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlockHusk blockHusk = (BlockHusk) o;
        return Objects.equals(block, blockHusk.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block);
    }
}
