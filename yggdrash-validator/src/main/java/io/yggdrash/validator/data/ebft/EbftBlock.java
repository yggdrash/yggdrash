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

package io.yggdrash.validator.data.ebft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.ConsensusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class EbftBlock implements ConsensusBlock {
    private static final Logger log = LoggerFactory.getLogger(EbftBlock.class);

    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public EbftBlock(Block block, List<String> consensusList) {
        this.block = block;
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public EbftBlock(Block block) {
        this(block, null);
    }

    public EbftBlock(byte[] bytes) {
        this(JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8)));
    }

    public EbftBlock(JsonObject jsonObject) {
        this.block = new Block(jsonObject.get("block").getAsJsonObject());

        JsonElement consensusJsonElement = jsonObject.get("consensusList");
        if (consensusJsonElement != null) {
            for (JsonElement jsonElement : consensusJsonElement.getAsJsonArray()) {
                this.consensusList.add(jsonElement.getAsString());
            }
        }
    }

    public EbftBlock(EbftProto.EbftBlock block) {
        this.block = Block.toBlock(block.getBlock());
        if (block.getConsensusList().getConsensusListList() != null) {
            for (String consensus : block.getConsensusList().getConsensusListList()) {
                if (consensus != null) {
                    this.consensusList.add(consensus);
                }
            }
        }
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public List<String> getConsensusMessages() {
        return consensusList;
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
        if (this.block == null) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", this.block.toJsonObject());
        if (this.consensusList.size() > 0) {
            JsonArray consensusJsonArray = new JsonArray();
            for (String consensus : consensusList) {
                consensusJsonArray.add(consensus);
            }
            jsonObject.add("consensusList", consensusJsonArray);
        }
        return jsonObject;
    }

    @Override
    public boolean equals(ConsensusBlock consensusBlock) {
        return this.block.equals(consensusBlock.getBlock())
                && Arrays.equals(this.consensusList.toArray(),
                ((List) consensusBlock.getConsensusMessages()).toArray());
    }

    @Override
    public void clear() {
        this.block.clear();
        this.consensusList.clear();
    }

    @Override
    public EbftBlock clone() {
        return new EbftBlock(this.toJsonObject());
    }

    @Override
    public boolean verify() {
        if (this.block == null) {
            return false;
        }

        // todo: check consensuses whether validator's signatures or not
        return this.block.verify();
    }

    public static boolean verify(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return false;
        }
        // todo: check consensuses whether validator's signatures or not
        return ebftBlock.getBlock().verify();
    }

    public static EbftProto.EbftBlock toProto(EbftBlock ebftBlock) {
        if (ebftBlock == null || ebftBlock.getBlock() == null
                || ebftBlock.getBlock().getHeader() == null) {
            return null;
        }

        EbftProto.EbftBlock.Builder protoBlock = EbftProto.EbftBlock.newBuilder()
                .setBlock(Block.toProtoBlock(ebftBlock.getBlock()))
                .setConsensusList(EbftProto.ConsensusList.newBuilder()
                        .addAllConsensusList(ebftBlock.getConsensusMessages()).build());
        return protoBlock.build();
    }

    public static EbftProto.EbftBlockList toProtoList(Collection<EbftBlock> collection) {
        EbftProto.EbftBlockList.Builder builder = EbftProto.EbftBlockList.newBuilder();
        for (EbftBlock ebftBlock : collection) {
            builder.addEbftBlockList(EbftBlock.toProto(ebftBlock));
        }
        return builder.build();
    }

}