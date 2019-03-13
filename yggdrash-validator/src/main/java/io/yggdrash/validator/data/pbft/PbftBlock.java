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
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.ConsensusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PbftBlock implements ConsensusBlock {
    private static final Logger log = LoggerFactory.getLogger(PbftBlock.class);

    private final Block block;
    private final PbftMessageSet pbftMessageSet;

    public PbftBlock(Block block, PbftMessageSet pbftMessageSet) {
        this.block = block;
        this.pbftMessageSet = pbftMessageSet;
    }

    public PbftBlock(byte[] bytes) {
        this(JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8)));
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
        return this.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
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

    public static PbftProto.PbftBlock toProto(PbftBlock pbftBlock) {
        Proto.Block protoBlock = pbftBlock.getBlock().toProtoBlock();
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