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
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.node.mock.NodeManagerMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static io.yggdrash.config.Constants.PROPERTY_KEYPATH;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class NodeManagerTest {

    private NodeManager nodeManager;
    private Transaction tx;
    private Block genesisBlock;
    private Block block;

    @Before
    public void setUp() throws Exception {
        nodeManager = new NodeManagerMock();
        Wallet wallet = nodeManager.getWallet();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(wallet, json);
        BlockBody sampleBody = new BlockBody(Arrays.asList(new Transaction[] {tx}));

        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(wallet);
        this.genesisBlock = new Block(genesisBlockHeader, sampleBody);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(genesisBlock) // genesis block
                .build(wallet);

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

    @Test
    public void defaultConfigTest() {
        DefaultConfig defaultConfig = nodeManager.getDefaultConfig();

        assertThat(defaultConfig.getConfig().getString("java.version"), containsString("1.8"));
        System.out.println("DefaultConfig java.version: "
                + defaultConfig.getConfig().getString("java.version"));

        assertThat(defaultConfig.getConfig().getString("node.name"), containsString("yggdrash"));
        System.out.println("DefaultConfig node.name: "
                + defaultConfig.getConfig().getString("node.name"));

        assertThat(defaultConfig.getConfig().getString("network.port"), containsString("31212"));
        System.out.println("DefaultConfig network.port: "
                + defaultConfig.getConfig().getString("network.port"));
    }

    @Test
    public void defaultWalletTest() {
        Wallet wallet = nodeManager.getWallet();

        assertNotNull(wallet);

        DefaultConfig config = nodeManager.getDefaultConfig();
        Path path = Paths.get(config.getConfig().getString(PROPERTY_KEYPATH));
        String keyPath = path.getParent().toString();
        String keyName = path.getFileName().toString();

        System.out.println("walletKeyPath: " + wallet.getKeyPath());
        System.out.println("walletKeyName: " + wallet.getKeyName());

        System.out.println("configKeyPath: " + keyPath);
        System.out.println("configKeyName: " + keyName);

        assertEquals(wallet.getKeyPath(), keyPath);
        assertEquals(wallet.getKeyName(), keyName);
    }

}
