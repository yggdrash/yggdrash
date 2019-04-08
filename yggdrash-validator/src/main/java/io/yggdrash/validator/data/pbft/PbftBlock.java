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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.ConsensusBlock;

import java.util.Arrays;
import java.util.List;

public class PbftBlock implements ConsensusBlock<PbftProto.PbftBlock> {

    private PbftProto.PbftBlock protoBlock;

    private final transient Block block;
    private final transient PbftMessageSet pbftMessageSet;

    public PbftBlock(Block block, PbftMessageSet pbftMessageSet) {
        this.block = block;
        this.pbftMessageSet = pbftMessageSet;
    }

    public PbftBlock(byte[] bytes) {
        try {
            this.protoBlock = PbftProto.PbftBlock.parseFrom(bytes);
            this.block = Block.toBlock(protoBlock.getBlock());
            this.pbftMessageSet = new PbftMessageSet(protoBlock.getPbftMessageSet());
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    public PbftBlock(JsonObject jsonObject) {
        this.block = new Block(jsonObject.get("block").getAsJsonObject());

        JsonElement pbftMessageSetJsonElement = jsonObject.get("pbftMessageSet");
        if (pbftMessageSetJsonElement != null) {
            this.pbftMessageSet = new PbftMessageSet(pbftMessageSetJsonElement.getAsJsonObject());
        } else {
            this.pbftMessageSet = null;
        }
    }

    public PbftBlock(PbftProto.PbftBlock protoBlock) {
        this.protoBlock = protoBlock;
        this.block = Block.toBlock(protoBlock.getBlock());
        this.pbftMessageSet = new PbftMessageSet(protoBlock.getPbftMessageSet());
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public PbftMessageSet getConsensusMessages() {
        return pbftMessageSet;
    }

    @Override
    public byte[] getChain() {
        return this.block.getChain();
    }

    @Override
    public long getIndex() {
        return this.block.getIndex();
    }

    @Override
    public byte[] getHash() {
        return this.block.getHash();
    }

    @Override
    public String getHashHex() {
        return this.block.getHashHex();
    }

    @Override
    public byte[] getPrevBlockHash() {
        return this.block.getPrevBlockHash();
    }

    @Override
    public byte[] toBinary() {
        return SerializationUtil.serializeJson(toJsonObject());
    }

    @Override
    public byte[] getData() {
        return getInstance().toByteArray();
    }

    @Override
    public PbftProto.PbftBlock getInstance() {
        if (protoBlock != null) {
            return protoBlock;
        }
        protoBlock = toProto();
        return protoBlock;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", this.block.toJsonObject());
        if (this.pbftMessageSet != null) {
            jsonObject.add("pbftMessageSet", this.pbftMessageSet.toJsonObject());
        }
        return jsonObject;
    }

    @Override
    public boolean equals(ConsensusBlock consensusBlock) {
        if (consensusBlock == null) {
            return false;
        }
        return Arrays.equals(this.toBinary(), consensusBlock.toBinary());
    }

    @Override
    public void clear() {
        this.block.clear();
        if (this.pbftMessageSet != null) {
            this.pbftMessageSet.clear();
        }
    }

    @Override
    public PbftBlock clone() {
        return new PbftBlock(this.toJsonObject());
    }

    @Override
    public boolean verify() {
        if (this.block == null) {
            return false;
        } else if (this.block.getIndex() == 0) {
            return this.block.verify();
        } else if (this.pbftMessageSet == null) {
            return false;
        } else {
            return this.block.verify()
                    && PbftMessageSet.verify(this.pbftMessageSet);
        }
    }

    public static boolean verify(PbftBlock block) {
        if (block == null || block.getBlock() == null) {
            return false;
        } else if (block.getIndex() == 0) {
            return block.getBlock().verify();
        } else if (block.getConsensusMessages() == null) {
            return false;
        } else {
            return block.getBlock().verify()
                    && PbftMessageSet.verify(block.getConsensusMessages());
        }
    }

    private PbftProto.PbftBlock toProto() {
        return toProto(this);
    }

    public static PbftProto.PbftBlock toProto(PbftBlock pbftBlock) {
        if (pbftBlock == null || pbftBlock.getBlock() == null) {
            return null;
        }

        Proto.Block protoBlock = Block.toProtoBlock(pbftBlock.getBlock());
        PbftProto.PbftMessageSet protoPbftMessageSet =
                PbftMessageSet.toProto(pbftBlock.getConsensusMessages());

        PbftProto.PbftBlock.Builder protoPbftBlockBuilder = PbftProto.PbftBlock.newBuilder();
        if (protoBlock != null) {
            protoPbftBlockBuilder.setBlock(protoBlock);
        }
        if (protoPbftMessageSet != null) {
            protoPbftBlockBuilder.setPbftMessageSet(protoPbftMessageSet);
        }

        return protoPbftBlockBuilder.build();
    }

    public static PbftProto.PbftBlockList toProtoList(List<PbftBlock> pbftBlockList) {
        if (pbftBlockList == null) {
            return null;
        }

        PbftProto.PbftBlockList.Builder protoPbftBlockListBuilder =
                PbftProto.PbftBlockList.newBuilder();
        for (PbftBlock pbftBlock : pbftBlockList) {
            protoPbftBlockListBuilder.addPbftBlock(PbftBlock.toProto(pbftBlock));
        }

        return protoPbftBlockListBuilder.build();
    }
}