/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.mapper.BlockMapper;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.util.SerializeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private Block block;

    @Before
    public void setUp() throws Exception {
        Wallet wallet = new Wallet();

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("operator", "transfer");
        jsonObject1.addProperty("to", "0x5186a0EF662DFA89Ed44b52a55EC5Cf0B4b59bb7");
        jsonObject1.addProperty("balance", "100000000");
        Transaction tx1 = new Transaction(wallet, jsonObject1);

        List<Transaction> txs = new ArrayList<>();
        txs.add(tx1);

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("operator", "transfer");
        jsonObject2.addProperty("to", "0x3386a0EF662DFA89Ed44b52a55EC5Cf0B4b59bb7");
        jsonObject2.addProperty("balance", "200000000");
        Transaction tx2 = new Transaction(wallet, jsonObject2);
        txs.add(tx2);

        BlockBody sampleBody = new BlockBody(txs);

        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(wallet);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(new Block(genesisBlockHeader, sampleBody)) // genesis block
                .build(wallet);
        this.block = new Block(blockHeader, sampleBody);
    }

    @Test
    public void blockTest() {
        assert !block.getBlockHash().isEmpty();
        assert block.getIndex() == 1;
    }

    @Test
    public void deserializeBlockFromSerializerTest() throws IOException, ClassNotFoundException {
        byte[] bytes = SerializeUtils.convertToBytes(block);
        assert bytes != null;
        ByteString byteString = ByteString.copyFrom(bytes);
        byte[] byteStringBytes = byteString.toByteArray();
        assert bytes.length == byteStringBytes.length;
        Block deserializeBlock = (Block) SerializeUtils.convertFromBytes(byteStringBytes);
        assert deserializeBlock != null;
        assert block.getBlockHash().equals(deserializeBlock.getBlockHash());
    }

    @Test
    public void deserializeTransactionFromProtoTest() {
        BlockChainProto.Block protoBlock = BlockMapper.blockToProtoBlock(block);
        Block deserializeBlock = BlockMapper.protoBlockToBlock(protoBlock);
        assert block.getBlockHash().equals(deserializeBlock.getBlockHash());
    }

    @Test
    public void testToJsonObject() {
        //todo: modify to checking jsonObject when the block data format change to JsonObject.
        try {
            log.debug(block.toJsonObject().toString());
        } catch (Exception e) {
            assert false;
        }
    }

}
