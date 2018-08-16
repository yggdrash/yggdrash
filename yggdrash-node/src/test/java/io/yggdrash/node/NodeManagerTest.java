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
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionValidator;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.store.HashMapTransactionPool;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.util.ByteUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class NodeManagerTest {

    private static final Logger log = LoggerFactory.getLogger(NodeManagerTest.class);

    private NodeManagerImpl nodeManager;
    private NodeProperties nodeProperties;
    private Transaction tx;
    private Block firstBlock;
    private Block secondBlock;

    @Before
    public void setUp() throws Exception {
        this.nodeProperties = new NodeProperties();
        nodeProperties.getGrpc().setHost("localhost");
        nodeProperties.getGrpc().setPort(9090);
        this.nodeManager = new NodeManagerImpl();
        nodeManager.setPeerGroup(new PeerGroup());
        nodeManager.setNodeProperties(nodeProperties);
        MessageSender<PeerClientChannel> messageSender = new MessageSender<>(nodeProperties);
        messageSender.setListener(nodeManager);
        nodeManager.setMessageSender(messageSender);
        nodeManager.setWallet(new Wallet());
        nodeManager.setTxValidator(new TransactionValidator());

        TransactionStore transactionStore = new TransactionStore(new HashMapDbSource(),
                new HashMapTransactionPool());

        nodeManager.setTransactionStore(transactionStore);
        nodeManager.setBlockChain(new BlockChain());
        nodeManager.setBlockBuilder(new BlockBuilderImpl());
        nodeManager.setNodeHealthIndicator(mock(NodeHealthIndicator.class));
        nodeManager.init();
        assert nodeManager.getNodeUri() != null;
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(nodeManager.getWallet(), json);
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader firstBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(nodeManager.getBlockChain().getPrevBlock())
                .build(nodeManager.getWallet());
        this.firstBlock = new Block(firstBlockHeader, sampleBody);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(firstBlock) // genesis block
                .build(nodeManager.getWallet());

        this.secondBlock = new Block(blockHeader, sampleBody);
    }

    @Test
    public void addTransactionTest() {
        nodeManager.addTransaction(tx);
        Transaction pooledTx = nodeManager.getTxByHash(tx.getHashString());
        assert pooledTx.getHashString().equals(tx.getHashString());
    }

    @Test
    public void addBlockTest() {
        nodeManager.addTransaction(tx);
        nodeManager.addBlock(firstBlock);
        nodeManager.addBlock(secondBlock);
        assert nodeManager.getBlocks().size() == 2;
        assert nodeManager.getBlockByIndexOrHash("1").getBlockHash()
                .equals(secondBlock.getBlockHash());
        Transaction foundTx = nodeManager.getTxByHash(tx.getHashString());
        assert foundTx.getHashString().equals(tx.getHashString());
    }

    @Test
    public void generateBlockTest() {
        nodeManager.addTransaction(tx);
        Block newBlock = nodeManager.generateBlock();
        assert nodeManager.getBlocks().size() == 1;
        Block chainedBlock = nodeManager.getBlockByIndexOrHash(newBlock.getBlockHash());
        assert chainedBlock.getBlockHash().equals(newBlock.getBlockHash());
        log.debug(Hex.toHexString(ByteUtil.longToBytes(chainedBlock.getData().getSize())));
        assert chainedBlock.getData().getSize() != 0;
        assertThat(nodeManager.getTxByHash(tx.getHashString()).getHashString(),
                is(tx.getHashString()));
    }

    @Test
    public void addPeerTest() {
        int testCount = nodeProperties.getMaxPeers() + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 9000;
            nodeManager.addPeer("ynode://75bff16c@localhost:" + port);
        }
        assert nodeProperties.getMaxPeers() == nodeManager.getPeerUriList().size();
    }
}
