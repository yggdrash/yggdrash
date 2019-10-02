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
import io.yggdrash.core.consensus.AbstractConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.PbftProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbftBlockMock extends AbstractConsensusBlock<PbftProto.PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockMock.class);

    public PbftBlockMock(byte[] bytes) {
        this(toProto(bytes));
    }

    public PbftBlockMock(Block block) {
        super(block);
    }

    @Override
    public Object getConsensusMessages() {
        return "MockConsensus";
    }

    @Override
    public JsonObject getConsensusMessagesJsonObject() {
        return new JsonObject();
    }

    @Override
    public PbftProto.PbftBlock getInstance() {
        return PbftProto.PbftBlock.newBuilder().setBlock(getProtoBlock()).build();
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

    private static Block toProto(byte[] bytes) {
        try {
            return new BlockImpl(PbftProto.PbftBlock.parseFrom(bytes).getBlock());
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public void loggingBlock(int unConfirmedTxs) {
        try {
            log.info("PbftBlockMock");
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
