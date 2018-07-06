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

package io.yggdrash.node;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.node.mock.NodeManagerMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class NodeManagerTest {

    private NodeManager nodeManager;
    private Transaction tx;
    private Block genesisBlock;
    private Block block;

    @Before
    public void setUp() throws Exception {
        nodeManager = new NodeManagerMock();
        Account author = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(author, json);
        BlockBody sampleBody = new BlockBody(Arrays.asList(new Transaction[] {tx}));

        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(author);
        this.genesisBlock = new Block(genesisBlockHeader, sampleBody);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(genesisBlock) // genesis block
                .build(author);

        this.block = new Block(blockHeader, sampleBody);
    }

    @Test
    public void addTransactionTest() throws Exception {
        nodeManager.addTransaction(tx);
        Transaction pooledTx = nodeManager.getTxByHash(tx.getHashString());
        assert pooledTx.getHashString().equals(tx.getHashString());
    }

    @Test
    public void addBlockTest() throws IOException, NotValidteException {
        nodeManager.addTransaction(tx);
        nodeManager.addBlock(genesisBlock);
        nodeManager.addBlock(block);
        assert nodeManager.getBlocks().size() == 2;
        assert nodeManager.getBlockByIndexOrHash("1").getBlockHash().equals(block.getBlockHash());
        assert nodeManager.getTxByHash(tx.getHashString()) == null;
    }

    @Test
    public void generateBlockTest() throws IOException, NotValidteException {
        nodeManager.addTransaction(tx);
        Block newBlock = nodeManager.generateBlock();
        assert nodeManager.getBlocks().size() == 1;
        Block chainedBlock =  nodeManager.getBlockByIndexOrHash(newBlock.getBlockHash());
        assert chainedBlock.getBlockHash().equals(newBlock.getBlockHash());
        assert chainedBlock.getData().getSize() == 1;
        assert nodeManager.getTxByHash(tx.getHashString()) == null;
    }
}
