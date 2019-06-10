/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.consensus.AbstractConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.PbftProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbftBlock extends AbstractConsensusBlock<PbftProto.PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlock.class);

    private transient PbftMessageSet pbftMessageSet;

    public PbftBlock(byte[] bytes) {
        this(toProto(bytes));
    }

    public PbftBlock(PbftProto.PbftBlock block) {
        this(new BlockImpl(block.getBlock()), new PbftMessageSet(block.getPbftMessageSet()));
    }

    public PbftBlock(Block block, PbftMessageSet pbftMessageSet) {
        super(block);
        this.pbftMessageSet = pbftMessageSet;
    }

    public PbftBlock(JsonObject jsonObject) {
        this(new BlockImpl(jsonObject.get("block").getAsJsonObject()),
                new PbftMessageSet(jsonObject.get("pbftMessageSet").getAsJsonObject()));
    }

    @Override
    public PbftMessageSet getConsensusMessages() {
        return pbftMessageSet;
    }

    @Override
    public JsonObject getConsensusMessagesJsonObject() {
        return pbftMessageSet.toJsonObject();
    }

    @Override
    public PbftProto.PbftBlock getInstance() {
        return PbftProto.PbftBlock.newBuilder()
                .setBlock(getProtoBlock())
                .setPbftMessageSet(PbftMessageSet.toProto(pbftMessageSet)).build();
    }

    @Override
    public byte[] toBinary() {
        return getInstance().toByteArray();
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", getBlock().toJsonObject());
        if (this.pbftMessageSet != null) {
            jsonObject.add("pbftMessageSet", this.pbftMessageSet.toJsonObject());
        }
        return jsonObject;
    }

    @Override
    public void clear() {
        this.pbftMessageSet.clear();
    }

    private static PbftProto.PbftBlock toProto(byte[] bytes) {
        try {
            return PbftProto.PbftBlock.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public void loggingBlock() {
        try {
            log.info("PbftBlock ({}) [{}] ({}) ({}) ({}) ({}) ({}) tx({})",
                    this.getConsensusMessages().getPrePrepare().getViewNumber(),
                    this.getIndex(),
                    this.getHash(),
                    this.getConsensusMessages().getPrepareMap().size(),
                    this.getConsensusMessages().getCommitMap().size(),
                    this.getConsensusMessages().getViewChangeMap().size(),
                    this.getBlock().getAddress(),
                    this.getBlock().getBody().getCount()
            );
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}