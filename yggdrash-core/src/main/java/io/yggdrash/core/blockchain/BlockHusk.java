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
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.core.consensus.AbstractBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;

@Deprecated
public class BlockHusk extends AbstractBlock<Proto.Block> {

    public BlockHusk(byte[] bytes) {
        this(toProto(bytes));
    }

    public BlockHusk(io.yggdrash.core.blockchain.Block block) {
        super(block);
    }

    @Override
    public Object getConsensusMessages() {
        throw new FailedOperationException("Not implemented");
    }

    @Override
    public Proto.Block getInstance() {
        return getBlock().getInstance();
    }

    @Override
    public byte[] toBinary() {
        return getInstance().toByteArray();
    }

    @Override
    public JsonObject toJsonObject() {
        return getBlock().toJsonObject();
    }

    @Override
    public void clear() {
        throw new FailedOperationException("Not implemented");
    }

    private static io.yggdrash.core.blockchain.Block toProto(byte[] bytes) {
        try {
            return new Block(Proto.Block.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}
