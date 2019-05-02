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
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.consensus.AbstractConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.EbftProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EbftBlock extends AbstractConsensusBlock<EbftProto.EbftBlock> {

    private final transient List<ByteString> consensusList;

    public EbftBlock(byte[] bytes) {
        this(toProto(bytes));
    }

    public EbftBlock(EbftProto.EbftBlock block) {
        this(new BlockImpl(block.getBlock()), block.getConsensusList().getConsensusList());
    }

    public EbftBlock(Block block) {
        this(block, Collections.emptyList());
    }

    public EbftBlock(Block block, List<ByteString> consensusList) {
        super(block);
        this.consensusList = new ArrayList<>(consensusList);
    }

    @Override
    public List<ByteString> getConsensusMessages() {
        return consensusList;
    }

    @Override
    public EbftProto.EbftBlock getInstance() {
        EbftProto.ConsensusList list = EbftProto.ConsensusList.newBuilder().addAllConsensus(consensusList).build();
        return EbftProto.EbftBlock.newBuilder().setBlock(getProtoBlock()).setConsensusList(list).build();
    }

    @Override
    public byte[] toBinary() {
        return getInstance().toByteArray();
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", getBlock().toJsonObject());
        if (!consensusList.isEmpty()) {
            JsonArray consensusJsonArray = new JsonArray();
            for (ByteString consensus : consensusList) {
                consensusJsonArray.add(consensus.toString());
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

    private static EbftProto.EbftBlock toProto(byte[] bytes) {
        try {
            return EbftProto.EbftBlock.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}