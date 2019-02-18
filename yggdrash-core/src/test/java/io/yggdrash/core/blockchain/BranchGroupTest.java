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

import com.google.gson.JsonArray;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.core.exception.DuplicatedException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static io.yggdrash.TestConstants.PerformanceTest;
import static io.yggdrash.TestConstants.TRANSFER_TO;
import static io.yggdrash.TestConstants.wallet;
import static io.yggdrash.TestConstants.yggdrash;
import static org.assertj.core.api.Assertions.assertThat;

public class BranchGroupTest {

    private BranchGroup branchGroup;
    private TransactionHusk tx;
    private BlockHusk block;
    protected static final Logger log = LoggerFactory.getLogger(BranchGroupTest.class);

    @Before
    public void setUp() {
        branchGroup = BlockChainTestUtils.createBranchGroup();
        tx = BlockChainTestUtils.createBranchTxHusk();
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        BlockChain bc = branchGroup.getBranch(tx.getBranchId());
        block = newBlock(Collections.singletonList(tx), bc.getPrevBlock());
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        BlockChain exist = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(exist);
    }

    @Test
    public void addTransaction() {
        // should be existed tx on genesis block
        assertThat(branchGroup.getRecentTxs(tx.getBranchId()).size()).isEqualTo(1);
        assertThat(branchGroup.countOfTxs(tx.getBranchId())).isEqualTo(1);

        branchGroup.addTransaction(tx);
        TransactionHusk foundTxBySha3 = branchGroup.getTxByHash(
                tx.getBranchId(), tx.getHash());
        assertThat(foundTxBySha3.getHash()).isEqualTo(tx.getHash());

        TransactionHusk foundTxByString = branchGroup.getTxByHash(
                tx.getBranchId(), tx.getHash().toString());
        assertThat(foundTxByString.getHash()).isEqualTo(tx.getHash());

        assertThat(branchGroup.getUnconfirmedTxs(tx.getBranchId()).size()).isEqualTo(1);
    }

    @Test
    public void generateBlock() {
        branchGroup.addTransaction(tx);
        branchGroup.generateBlock(wallet(), tx.getBranchId());
        long latest = branchGroup.getLastIndex(tx.getBranchId());
        BlockHusk chainedBlock = branchGroup.getBlockByIndex(tx.getBranchId(), latest);
        assertThat(latest).isEqualTo(1);
        assertThat(chainedBlock.getBody().size()).isEqualTo(1);
        assertThat(branchGroup.getTxByHash(tx.getBranchId(), tx.getHash()).getHash())
                .isEqualTo(tx.getHash());
    }

    /**
     * test generate block with large tx.
     */
    @Test(timeout = 5000L)
    public void generateBlockPerformanceTest() {
        PerformanceTest.apply();
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        for (int i = 0; i < 100; i++) {
            TransactionHusk tx = createTx(i);
            blockChain.addTransaction(tx);
        }

        branchGroup.generateBlock(wallet(), blockChain.getBranchId());
    }

    @Test
    public void addBlock() {
        branchGroup.addTransaction(tx);
        branchGroup.addBlock(block);

        BlockHusk newBlock = newBlock(Collections.singletonList(tx), block);
        branchGroup.addBlock(newBlock);

        assertThat(branchGroup.getLastIndex(newBlock.getBranchId())).isEqualTo(2);
        assertThat(branchGroup.getBlockByIndex(newBlock.getBranchId(), 2).getHash())
                .isEqualTo(newBlock.getHash());
        TransactionHusk foundTx = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
        assertThat(foundTx.getHash()).isEqualTo(tx.getHash());
    }

    @Test
    public void getStateStore() {
        assertThat(branchGroup.getStateStore(block.getBranchId())).isNotNull();
    }

    @Test
    public void getTransactionReceiptStore() {
        assertThat(branchGroup.getTransactionReceiptStore(tx.getBranchId())).isNotNull();
    }

    private TransactionHusk createTx(int amount) {
        JsonArray txBody = ContractTestUtils.transferTxBodyJson(TRANSFER_TO, amount);
        return BlockChainTestUtils.createTxHusk(yggdrash(), txBody);
    }

    private BlockHusk newBlock(List<TransactionHusk> body, BlockHusk prevBlock) {
        return new BlockHusk(wallet(), body, prevBlock);
    }
}
