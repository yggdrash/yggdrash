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

import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BlockHuskBuilder;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.Runtime;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class NodeManagerTest {

    private static final Logger log = LoggerFactory.getLogger(NodeManagerTest.class);

    private GRpcNodeServer nodeManager;
    private BranchGroup branchGroup;
    private NodeProperties nodeProperties;
    private TransactionHusk tx;
    private BlockHusk firstBlock;
    private BlockHusk secondBlock;
    private PeerGroup peerGroup;

    @Before
    public void setUp() throws Exception {
        this.nodeManager = new GRpcNodeServer();
        this.nodeProperties = new NodeProperties();
        this.peerGroup = new PeerGroup(nodeProperties.getMaxPeers());
        Runtime runtime = new Runtime(new StateStore(), new TransactionReceiptStore());
        this.branchGroup = new BranchGroup(runtime);
        BlockChain blockChain = new BlockChain(
                new File(getClass().getClassLoader()
                        .getResource("branch-yeed.json").getFile()));
        branchGroup.addBranch(blockChain.getBranchId(), blockChain);

        nodeManager.setWallet(new Wallet());
        nodeManager.setPeerGroup(peerGroup);
        nodeManager.setBranchGroup(branchGroup);
        nodeManager.setNodeHealthIndicator(mock(NodeHealthIndicator.class));

        nodeManager.start("localhost", 0);
        assert nodeManager.getNodeUri() != null;

        this.tx = TestUtils.createTxHusk(nodeManager.getWallet());
        this.firstBlock = BlockHuskBuilder.buildUnSigned(nodeManager.getWallet(),
                Collections.singletonList(tx), branchGroup.getBlockByIndexOrHash("0"));
        this.secondBlock = BlockHuskBuilder.buildSigned(nodeManager.getWallet(),
                Collections.singletonList(tx), firstBlock);
    }

    @After
    public void tearDown() {
        //TODO 테스트 설정 파일에서 DB 부분 제거
        FileUtil.recursiveDelete(Paths.get(".yggdrash/db"));
    }

    @Test
    public void addTransactionTest() {
        branchGroup.addTransaction(tx);
        TransactionHusk pooledTx = branchGroup.getTxByHash(tx.getHash());
        assert pooledTx.getHash().equals(tx.getHash());
    }

    @Test(expected = InvalidSignatureException.class)
    public void unsignedTxTest() {
        branchGroup.addTransaction(new TransactionHusk(TestUtils.getTransactionFixture()));
    }

    @Test
    public void addBlockTest() {
        branchGroup.addTransaction(tx);
        branchGroup.addBlock(firstBlock);
        branchGroup.addBlock(secondBlock);
        assert branchGroup.getBlocks().size() == 3;
        assert branchGroup.getBlockByIndexOrHash("2").getHash()
                .equals(secondBlock.getHash());
        TransactionHusk foundTx = branchGroup.getTxByHash(tx.getHash());
        assert foundTx.getHash().equals(tx.getHash());
    }

    @Test
    public void generateBlockTest() {
        branchGroup.addTransaction(tx);
        BlockHusk newBlock = branchGroup.generateBlock(nodeManager.getWallet());
        assert branchGroup.getBlocks().size() == 2;
        BlockHusk chainedBlock = branchGroup.getBlockByIndexOrHash(newBlock.getHash().toString());
        assert chainedBlock.getHash().equals(newBlock.getHash());
        log.debug(Hex.toHexString(ByteUtil.longToBytes(chainedBlock.getBody().size())));
        assert chainedBlock.getBody().size() != 0;
        assertThat(branchGroup.getTxByHash(tx.getHash()).getHash(), equalTo(tx.getHash()));
    }
}
