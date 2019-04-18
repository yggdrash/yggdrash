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

import io.grpc.ManagedChannel;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.p2p.Peer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class RpcTest extends TcpNodeTesting {
    private static final Logger log = LoggerFactory.getLogger(RpcTest.class);
    private static final int NODE_CNT = 2;

    private List<Block> blockList;
    private List<TransactionHusk> txHuskList;
    private GRpcPeerHandler handler;

    @Override
    public void setUp() {
        super.setUp();
        TestConstants.SlowTest.apply();

        bootstrapNodes(NODE_CNT, true);

        // Peer to send rpc msg
        Peer peer = nodeList.get(1).peerTableGroup.getOwner();
        ManagedChannel channel = createChannel(peer);
        handler = new GRpcPeerHandler(channel, peer);

        setBlockHuskList();
        setTxHuskList();

        log.debug("{} nodes bootstrapped", NODE_CNT);
        log.debug("BlockHuskList and TxHuskList are set: size of BlockHuskList={}, TxHuskList={}",
                blockList.size(), txHuskList.size());
    }

    private void setBlockHuskList() {
        blockList = new ArrayList<>();
        createBlockList(BlockChainTestUtils.createNextBlock());
    }

    private void createBlockList(Block blockHusk) {
        while (blockList.size() < 10) {
            blockList.add(blockHusk);
            createBlockList(BlockChainTestUtils.createNextBlock(blockHusk));
        }
    }

    private void setSpecificBlockHeightOfBlockChain(BlockChain branch) {
        log.debug("*** Set specific block height of blockChain ***");
        BlockChainTestUtils.setBlockHeightOfBlockChain(branch, 10);
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
        BlockChain branch = nodeList.get(1).getDefaultBranch();

        for (TransactionHusk txHusk : txHuskList) {
            branch.addTransaction(txHusk);
        }
    }

    @Test
    public void syncTxTest() throws Exception {
        addDummyTx();

        BlockChain branch = nodeList.get(1).getDefaultBranch();
        Future<List<TransactionHusk>> futureHusks = handler.syncTx(branch.getBranchId());

        List<TransactionHusk> txHusks = futureHusks.get();
        for (TransactionHusk txHusk : txHusks) {
            Assert.assertTrue(txHuskList.contains(txHusk));
        }
        Assert.assertEquals(txHusks.size(), txHuskList.size());
    }

    @Test
    public void syncBlockTest() throws Exception {
        BlockChain branch = nodeList.get(1).getDefaultBranch();
        setSpecificBlockHeightOfBlockChain(branch);

        Future<List<Block>> futureBlockList = handler.syncBlock(branch.getBranchId(), 5);

        List<Block> blockList = futureBlockList.get();
        for (Block blockHusk : blockList) {
            Assert.assertEquals(branch.getBlockByIndex(blockHusk.getIndex()), blockHusk);
        }
    }

    @Test
    public void broadcastBlockTest() {
        for (Block block : blockList) {
            handler.broadcastBlock(block);
        }
        handler.stop();
    }

    @Test
    public void broadcastTxTest() {
        for (TransactionHusk txHusk : getTmpTxList()) {
            handler.broadcastTx(txHusk);
        }
        handler.stop();
    }

    private List<TransactionHusk> getTmpTxList() {
        List<TransactionHusk> txList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            txList.add(BlockChainTestUtils.createTransferTxHusk());
        }
        return txList;
    }
}
