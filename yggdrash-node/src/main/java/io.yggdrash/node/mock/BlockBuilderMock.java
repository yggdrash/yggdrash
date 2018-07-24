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
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.BlockBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockBuilderMock implements BlockBuilder {

    private final NodeManager nodeManager;

    public BlockBuilderMock(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Deprecated
    @Override
    public Block build(List<Transaction> txList, Block prevBlock) throws IOException {
        BlockBody blockBody = new BlockBody(txList);
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(blockBody).build(nodeManager.getWallet());
        return new Block(blockHeader, blockBody);
    }

    @Override
    public Block build(Wallet wallet, List<Transaction> txList, Block prevBlock)
            throws IOException {

        BlockBody blockBody = new BlockBody(txList);
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(blockBody).build(wallet);
        return new Block(blockHeader, blockBody);
    }

    @Deprecated
    @Override
    public Block build() throws IOException {
        //todo: change logic for building the real block.

        // Create transactions
        JsonObject txObj1 = new JsonObject();
        JsonObject txObj2 = new JsonObject();
        JsonObject txObj3 = new JsonObject();

        txObj1.addProperty("operator", "transfer");
        txObj1.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj1.addProperty("value", 30);

        txObj2.addProperty("operator", "transfer");
        txObj2.addProperty("to", "0xdB44902E6cE92fa71Bbf06312630Cb39c5bE756C");
        txObj2.addProperty("value", 40);

        txObj3.addProperty("operator", "transfer");
        txObj3.addProperty("to", "0xA0A2fceBF3f3cc182eCfcbB65042Af0fB43dd864");
        txObj3.addProperty("value", 50);

        Transaction tx1 = new Transaction(this.nodeManager.getWallet(), txObj1);
        Transaction tx2 = new Transaction(this.nodeManager.getWallet(), txObj2);
        Transaction tx3 = new Transaction(this.nodeManager.getWallet(), txObj3);

        List<Transaction> txList = new ArrayList<>();
        txList.add(tx1);
        txList.add(tx2);
        txList.add(tx3);

        // Create a blockBody
        BlockBody blockBody = new BlockBody(txList);
        String blockBodyStr = blockBody.toString();

        // Create a blockHeader
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(null)
                .blockBody(blockBody).build(this.nodeManager.getWallet());

        // Return a created block
        return new Block(blockHeader, blockBody);
    }

    public Block build(Wallet wallet) throws IOException {
        //todo: change logic for building the real block.

        // Create transactions
        JsonObject txObj1 = new JsonObject();
        JsonObject txObj2 = new JsonObject();
        JsonObject txObj3 = new JsonObject();

        txObj1.addProperty("operator", "transfer");
        txObj1.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj1.addProperty("value", 30);

        txObj2.addProperty("operator", "transfer");
        txObj2.addProperty("to", "0xdB44902E6cE92fa71Bbf06312630Cb39c5bE756C");
        txObj2.addProperty("value", 40);

        txObj3.addProperty("operator", "transfer");
        txObj3.addProperty("to", "0xA0A2fceBF3f3cc182eCfcbB65042Af0fB43dd864");
        txObj3.addProperty("value", 50);

        Transaction tx1 = new Transaction(wallet, txObj1);
        Transaction tx2 = new Transaction(wallet, txObj2);
        Transaction tx3 = new Transaction(wallet, txObj3);

        List<Transaction> txList = new ArrayList<>();
        txList.add(tx1);
        txList.add(tx2);
        txList.add(tx3);

        // Create a blockBody
        BlockBody blockBody = new BlockBody(txList);

        // Create a blockHeader
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(null)
                .blockBody(blockBody).build(wallet);

        // Return a created block
        return new Block(blockHeader, blockBody);
    }
}
