/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node;

import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.service.BlockChainService;
import io.yggdrash.node.service.DiscoveryService;
import io.yggdrash.node.springboot.grpc.GrpcServerBuilderConfigurer;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@RunWith(JUnit4.class)
public class RpcTest extends AbstractNodeTest {
    private static final Logger log = LoggerFactory.getLogger(RpcTest.class);
    private static final int NODE_CNT = 2;

    private final AbstractApplicationContext context = new GenericApplicationContext();

    private List<BlockHusk> blockHuskList;
    private List<TransactionHusk> txHuskList;
    private GRpcPeerHandler handler;
    private BranchId branchId;

    @Override
    public void setUp() {
        TestConstants.SlowTest.apply();

        super.setUp();
        context.refresh();

        bootstrapNodes(NODE_CNT, true);

        branchId = BlockChainTestUtils.genesisBlock().getBranchId();
        // Peer to send rpc msg
        Peer peer = nodeList.get(1).peerTableGroup.getOwner();
        ManagedChannel channel = createChannel(peer);
        handler = new GRpcPeerHandler(channel, peer);
        handler.setTestHelper(testHelper);

        setBlockHuskList();
        setTxHuskList();

        log.debug("{} nodes bootstrapped", NODE_CNT);
        log.debug("The branchId for testing: {}", branchId);
        log.debug("BlockHuskList and TxHuskList are set: size of BlockHuskList={}, TxHustList={}",
                blockHuskList.size(), txHuskList.size());
    }

    private void setBlockHuskList() {
        blockHuskList = new ArrayList<>();
        createBlockList(BlockChainTestUtils.createNextBlock());
    }

    private void createBlockList(BlockHusk blockHusk) {
        while (blockHuskList.size() < 10) {
            blockHuskList.add(blockHusk);
            createBlockList(BlockChainTestUtils.createNextBlock(blockHusk));
        }
    }

    private void setSpecificBlockHeightOfBlockChain() {
        log.debug("*** Set specific block height of blockChain ***");

        BranchGroup branchGroup = nodeList.get(1).getBranchGroup();
        BlockChain branch = branchGroup.getBranch(branchId);

        for (BlockHusk blockHusk : blockHuskList) {
            branch.addBlock(blockHusk, false);
        }
    }

    private void setTxHuskList() {
        txHuskList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String description = "TEST" + i;
            txHuskList.add(BlockChainTestUtils.createBranchTxHusk(description));
        }
    }

    private void addDummyTx() {
        log.debug("*** Add dummy txs ***");
        BranchGroup branchGroup = nodeList.get(1).getBranchGroup();
        BlockChain branch = branchGroup.getBranch(branchId);

        for (TransactionHusk txHusk : txHuskList) {
            branch.addTransaction(txHusk);
        }
    }

    @Test
    public void syncTxTest() throws Exception {
        addDummyTx();

        Future<List<TransactionHusk>> futureHusks = handler.syncTx(branchId);

        List<TransactionHusk> txHusks = futureHusks.get();
        for (TransactionHusk txHusk : txHusks) {
            Assert.assertTrue(txHuskList.contains(txHusk));
        }
        Assert.assertEquals(txHusks.size(), txHuskList.size());
    }

    @Test
    public void syncBlockTest() throws Exception {
        setSpecificBlockHeightOfBlockChain();

        Future<List<BlockHusk>> futureHusks = handler.syncBlock(branchId, 5);

        List<BlockHusk> blockHusks = futureHusks.get();
        for (BlockHusk blockHusk : blockHusks) {
            int index = (int) blockHusk.getIndex() - 1;
            Assert.assertEquals(blockHusk.getHash(), blockHuskList.get(index).getHash());
        }
    }

    @Test
    public void broadcastBlockTest() throws Exception {
        for (BlockHusk blockHusk : blockHuskList) {
            handler.broadcastBlock(blockHusk);
        }
        Thread.sleep(3000);
    }

    @Test
    public void broadcastTxTest() throws Exception {
        for (TransactionHusk txHusk : getTmpTxList()) {
            handler.broadcastTx(txHusk);
        }
        Thread.sleep(3000);
    }

    private List<TransactionHusk> getTmpTxList() {
        List<TransactionHusk> txList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            txList.add(BlockChainTestUtils.createTransferTxHusk());
        }
        return txList;
    }

    @Test
    public void biDirectTest() throws Exception {
        handler.biDirectTest(100, "test");
        Thread.sleep(3000);
    }

    @Override
    protected ManagedChannel createChannel(Peer peer) {
        return ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build();
    }

    @Override
    protected Server createAndStartServer(TestNode node) {
        GrpcServerBuilderConfigurer configurer = builder -> {
            builder.addService(new DiscoveryService(node.discoveryConsumer));
            if (node.blockChainConsumer != null) {
                builder.addService(new BlockChainService(node.blockChainConsumer));
            }
        };

        GrpcServerRunner runner = new GrpcServerRunner(configurer,
                ServerBuilder.forPort(node.port));
        runner.setApplicationContext(context);
        try {
            runner.run();
            return runner.getServer();
        } catch (Exception e) {
            return null;
        }
    }

    private GRpcPeerHandler.TestHelper testHelper = new GRpcPeerHandler.TestHelper() {
        @Override
        public void onMessage(Message message) {
            log.debug("[TestHelper] Message received: {}", message);
        }

        @Override
        public void onRpcError(Throwable exception) {
            log.debug("[TestHelper] Error occurred: {}", exception.getMessage());
        }
    };
}
