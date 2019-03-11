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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class EbftBlock implements io.yggdrash.validator.data.Block {
    private static final Logger log = LoggerFactory.getLogger(EbftBlock.class);

    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;
    private static final int MAX_VALIDATOR_COUNT = 100;

    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public EbftBlock(byte[] bytes) {
        this(JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8)));
    }

    public EbftBlock(JsonObject jsonObject) {
        this.block = new Block(jsonObject.get("block").getAsJsonObject());

        JsonArray consensusJsonArray = jsonObject.get("consensusList").getAsJsonArray();
        if (consensusJsonArray != null) {
            for (JsonElement jsonElement : consensusJsonArray) {
                this.consensusList.add(jsonElement.getAsString());
            }
        }
    }

    public EbftBlock(Block block) {
        this(block, null);
    }

    public EbftBlock(Block block, List<String> consensusList) {
        this.block = block;
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
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
    public boolean equals(io.yggdrash.validator.data.Block block) {
        return this.block.equals(block.getBlock())
                && Arrays.equals(this.consensusList.toArray(),
                ((List) block.getConsensusMessages()).toArray());
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
        // todo: check consensuses whether validator's signatures or not
        return getBlock().verify();
    }

    public static boolean verify(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return false;
        }
        // todo: check consensuses whether validator's signatures or not
        return ebftBlock.getBlock().verify();
    }

    public static EbftProto.EbftBlock toProto(EbftBlock ebftBlock) {
        EbftProto.EbftBlock.Builder protoBlock = EbftProto.EbftBlock.newBuilder()
                .setBlock(ebftBlock.getBlock().toProtoBlock())
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