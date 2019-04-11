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
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.consensus.AbstractBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.Proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EbftBlock extends AbstractBlock<EbftProto.EbftBlock> {

    private transient List<String> consensusList;

    public EbftBlock(EbftProto.EbftBlock block) {
        super(block);
    }

    public EbftBlock(byte[] bytes) {
        this(toProto(bytes));
    }

    public EbftBlock(Block block) {
        this(block, Collections.emptyList());
    }

    public EbftBlock(Block block, List<String> consensusList) {
        this(toProto(block, consensusList));
    }

    public EbftBlock(JsonObject jsonObject) {
        this(toProto(new Block(jsonObject.get("block").getAsJsonObject()),
                toConsensusList(jsonObject.get("consensusList"))));
    }

    @Override
    public void initConsensus() {
        this.consensusList = new ArrayList<>(getInstance().getConsensusList().getConsensusListList());
    }

    @Override
    public List<String> getConsensusMessages() {
        return consensusList;
    }

    @Override
    public Proto.Block getProtoBlock() {
        return getInstance().getBlock();
    }

    @Override
    public byte[] getData() {
        return getInstance().toByteArray();
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", getBlock().toJsonObject());
        if (!consensusList.isEmpty()) {
            JsonArray consensusJsonArray = new JsonArray();
            for (String consensus : consensusList) {
                consensusJsonArray.add(consensus);
            }
            jsonObject.add("consensusList", consensusJsonArray);
        }
        return jsonObject;
    }

    @Override
    public void clear() {
        this.consensusList.clear();
    }

    public static boolean verify(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return false;
        }
        // todo: check consensuses whether validator's signatures or not
        return ebftBlock.getBlock().verify();
    }

    private static List<String> toConsensusList(JsonElement consensusJsonElement) {
        List<String> consensusList = new ArrayList<>();
        if (consensusJsonElement == null) {
            return consensusList;
        }
        for (JsonElement jsonElement : consensusJsonElement.getAsJsonArray()) {
            consensusList.add(jsonElement.getAsString());
        }
        return consensusList;
    }

    private static EbftProto.EbftBlock toProto(byte[] bytes) {
        try {
            return EbftProto.EbftBlock.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    private static EbftProto.EbftBlock toProto(Block block, List<String> consensusList) {
        EbftProto.ConsensusList list = EbftProto.ConsensusList.newBuilder()
                .addAllConsensusList(consensusList).build();

        EbftProto.EbftBlock.Builder protoBlock = EbftProto.EbftBlock.newBuilder()
                .setBlock(Block.toProtoBlock(block))
                .setConsensusList(list);
        return protoBlock.build();
    }

    public static EbftProto.EbftBlock toProto(EbftBlock ebftBlock) {
        return toProto(ebftBlock.getData());
    }
}