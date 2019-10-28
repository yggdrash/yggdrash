/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.DuplicatedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.yggdrash.TestConstants.PerformanceTest;
import static io.yggdrash.TestConstants.TRANSFER_TO;
import static org.assertj.core.api.Assertions.assertThat;

public class BranchGroupTest {

    private BranchGroup branchGroup;
    private Transaction tx;
    private ConsensusBlock block;
    protected static final Logger log = LoggerFactory.getLogger(BranchGroupTest.class);

    @Before
    public void setUp() {
        branchGroup = BlockChainTestUtils.createBranchGroup();
        //tx = BlockChainTestUtils.createBranchTx(); //TODO Check createBranchTx(). Branch prop not exists.
        tx = BlockChainTestUtils.createTransferTx();
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        BlockChain bc = branchGroup.getBranch(tx.getBranchId());
        block = BlockChainTestUtils.createNextBlock(bc.getBlockChainManager().getLastConfirmedBlock());
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        BlockChain exist = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(exist);
    }

    @Test
    public void addVersioningTransaction() {
        Transaction tx = BlockChainTestUtils.createContractProposeTx(
                "8c65bc05e107aab9ceaa872bbbb2d96d57811de4", "activate");
        Map<String, List<String>> errLogs = branchGroup.addTransaction(tx);
        Assert.assertEquals(1, errLogs.size()); //{SystemError=[Validator verification failed]}
    }

    @Test
    public void addTransaction() {

        int contractSize = branchGroup.getBranch(tx.getBranchId()).getBranchContracts().size();

        // should be existed tx on genesis block
        assertThat(branchGroup.getRecentTxs(tx.getBranchId()).size()).isEqualTo(contractSize);

        Map<String, List<String>> errLogs = branchGroup.addTransaction(tx);
        if (getBalance(tx.getAddress().toString()).equals(BigInteger.ZERO)) {
            Assert.assertEquals(1, errLogs.size()); //no balance!
        } else {
            Assert.assertEquals(0, errLogs.size());
            Transaction foundTxBySha3 = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
            assertThat(foundTxBySha3.getHash()).isEqualTo(tx.getHash());
            Transaction foundTxByString = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash().toString());
            assertThat(foundTxByString.getHash()).isEqualTo(tx.getHash());
            assertThat(branchGroup.getUnconfirmedTxs(tx.getBranchId()).size()).isEqualTo(1);
            log.debug("Add valid tx to branchGroup. ErrorLog = {}", errLogs);
        }

        Transaction invalidTx1 = BlockChainTestUtils
                .createInvalidTransferTx(BranchId.of("696e76616c6964"), ContractVersion.of("696e76616c696420"));
        errLogs = branchGroup.addTransaction(invalidTx1);
        Assert.assertEquals(1, errLogs.size());
        Assert.assertTrue(errLogs.containsKey("SystemError"));
        Assert.assertTrue(errLogs.get("SystemError").contains("Branch doesn't exist"));
        log.debug("Add invalid tx to branchGroup. ErrorLog = {}", errLogs);

        Transaction invalidTx2 = BlockChainTestUtils
                .createInvalidTransferTx(ContractVersion.of("696e76616c696420"));
        errLogs = branchGroup.addTransaction(invalidTx2);
        Assert.assertEquals(1, errLogs.size());
        Assert.assertTrue(errLogs.containsKey("SystemError"));
        Assert.assertTrue(errLogs.get("SystemError").contains("ContractVersion doesn't exist"));
        log.debug("Add invalid tx to branchGroup. ErrorLog = {}", errLogs);
    }

    @Test
    public void generateBlock() {
        Map<String, List<String>> errLogs = branchGroup.addTransaction(tx);
        BlockChainTestUtils.generateBlock(branchGroup, tx.getBranchId());
        long latest = branchGroup.getLastIndex(tx.getBranchId());
        ConsensusBlock chainedBlock = branchGroup.getBlockByIndex(tx.getBranchId(), latest);
        assertThat(latest).isEqualTo(1);

        if (errLogs.size() == 0) {
            assertThat(chainedBlock.getBody().getCount()).isEqualTo(1);
            assertThat(branchGroup.getTxByHash(tx.getBranchId(), tx.getHash()).getHash())
                    .isEqualTo(tx.getHash());
        } else {
            assertThat(chainedBlock.getBody().getCount()).isEqualTo(0); //no balance!
        }
    }

    /**
     * test generate block with large tx.
     */
    @Test(timeout = 5000L)
    public void generateBlockPerformanceTest() {
        PerformanceTest.apply();
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        int countTx = 0;
        for (int i = 0; i < 100; i++) {
            Transaction tx = createTx(BigInteger.valueOf(i));
            Map<String, List<String>> result = blockChain.addTransaction(tx);
            if (result.isEmpty()) {
                countTx++;
            }
        }

        BlockChainTestUtils.generateBlock(branchGroup, blockChain.getBranchId());
    }

    @Test
    public void addBlock() {
        ContractManager contractManager = branchGroup.getBranch(tx.getBranchId()).getContractManager();
        Map<String, List<String>> errLogs = branchGroup.addTransaction(tx);
        branchGroup.addBlock(block);
        ConsensusBlock newBlock
                = BlockChainTestUtils.createNextBlock(Collections.singletonList(tx), block, contractManager);
        branchGroup.addBlock(newBlock);

        assertThat(branchGroup.getLastIndex(newBlock.getBranchId())).isEqualTo(2);
        assertThat(branchGroup.getBlockByIndex(newBlock.getBranchId(), 2).getHash())
                .isEqualTo(newBlock.getHash());

        Receipt receipt = branchGroup.getBranch(tx.getBranchId()).getBlockChainManager()
                .getReceipt(tx.getHash().toString());
        if (getBalance(tx.getAddress().toString()).equals(BigInteger.ZERO)) {
            assertThat(receipt.getStatus()).isEqualTo(ExecuteStatus.ERROR);
            Assert.assertEquals(1, errLogs.size()); // no balance !
        } else {
            assertThat(receipt.getStatus()).isNotEqualTo(ExecuteStatus.ERROR);
            Assert.assertEquals(0, errLogs.size());
            Transaction foundTx = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
            assertThat(foundTx.getHash()).isEqualTo(tx.getHash());
        }
    }

    @Test
    public void specificBlockHeightOfBlockChain() {
        addMultipleBlock(block);
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        assertThat(blockChain.getBlockChainManager().getLastIndex()).isEqualTo(10);
    }

    private void addMultipleBlock(ConsensusBlock block) {
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        while (blockChain.getBlockChainManager().getLastIndex() < 10) {
            log.debug("Last Index : {}", blockChain.getBlockChainManager().getLastIndex());
            branchGroup.addBlock(block);
            ConsensusBlock nextBlock = BlockChainTestUtils.createNextBlock(
                    Collections.emptyList(), block, blockChain.getContractManager());
            addMultipleBlock(nextBlock);
        }
    }

    private Transaction createTx(BigInteger amount) {
        return BlockChainTestUtils.createTransferTx(TRANSFER_TO, amount);
    }

    private BigInteger getBalance(String address) {
        JsonObject qryParam = new JsonObject();
        qryParam.addProperty("address", tx.getAddress().toString());
        return (BigInteger) branchGroup.query(BranchId.of(tx.getBranchId().toString()),
                TestConstants.YEED_CONTRACT.toString(), "balanceOf", qryParam);
    }
}
