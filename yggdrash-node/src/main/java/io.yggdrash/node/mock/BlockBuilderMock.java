/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.node.mock;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.BlockBuilder;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockBuilderMock implements BlockBuilder {
    @Override
    public Block build(List<Transaction> txList, Block prevBlock) throws IOException {
        Account account = new Account();
        BlockBody blockBody = new BlockBody(txList);
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(blockBody).build(account);
        return new Block(blockHeader, blockBody);
    }

    @Override
    public Block build() throws IOException {
        /* 1. Create an account */
        Account from = new Account();
        String fromAddress = Hex.encodeHexString(from.getAddress());

        /* 2. Create transactions */
        JsonObject txObj = new JsonObject();
        JsonObject txData = new JsonObject();

        txObj.addProperty("version", "0");
        txObj.addProperty("type", "00000000000000");
        txObj.addProperty("timestamp", "155810745733540");
        txObj.addProperty("from",
                "04a0cb0bc45c5889b8136127409de1ae7d3f668e5f29115730362823ed5223aff9"
                        + "b2c22210280af1249e27b08bdeb5c0160af74ec5237292b5ee94bd148c9aabbb");
        txObj.addProperty("dataHash",
                "ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485");
        txObj.addProperty("dataSize", "13");
        txObj.addProperty("signature",
                "b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d76"
                        + "8331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104");
        txObj.addProperty("transactionHash",
                "c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0");
        txObj.addProperty("transactionData", txData.toString());

        Transaction tx1 = new Transaction(from, txObj);
        Transaction tx2 = new Transaction(from, txObj);
        Transaction tx3 = new Transaction(from, txObj);

        List<Transaction> txList = new ArrayList<>();
        txList.add(tx1);
        txList.add(tx2);
        txList.add(tx3);

        /* 3. Create a blockBody */
        BlockBody blockBody = new BlockBody(txList);
        String blockBodyStr = blockBody.toString();

        /* 4. Create a blockHeader */
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(null)
                .blockBody(blockBody).build(from);

        /* 5. Return a created block */
        return new Block(blockHeader, blockBody);
    }
}
