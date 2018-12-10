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
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.exception.DuplicatedException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchGroupTest {

    private BranchGroup branchGroup;
    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        branchGroup = new BranchGroup();
        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);
        addBranch(blockChain);
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        tx = BlockChainTestUtils.createBranchTxHusk();
        block = newBlock(Collections.singletonList(tx), blockChain.getPrevBlock());
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        addBranch(BlockChainTestUtils.createBlockChain(false));
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
        branchGroup.generateBlock(TestConstants.wallet(), tx.getBranchId());
        long latest = branchGroup.getLastIndex(tx.getBranchId());
        BlockHusk chainedBlock = branchGroup.getBlockByIndex(tx.getBranchId(), latest);
        assertThat(latest).isEqualTo(1);
        assertThat(chainedBlock.getBody().size()).isEqualTo(1);
        assertThat(branchGroup.getTxByHash(tx.getBranchId(), tx.getHash()).getHash())
                .isEqualTo(tx.getHash());
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

    @Test
    public void getContract() throws Exception {
        Contract contract = branchGroup.getContract(block.getBranchId());
        assertThat(contract).isNotNull();
        JsonObject query = ContractTestUtils.createQuery(block.getBranchId(),
                "getAllBranchId", new JsonObject());
        JsonObject resultObject = contract.query(query);
        String result = resultObject.get("result").getAsString();
        assertThat(result).contains(block.getBranchId().toString());
    }

    @Test
    public void query() {
        JsonObject param = ContractTestUtils.createParam("branchId",
                "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject query =
                ContractTestUtils.createQuery(block.getBranchId(), "view", param);
        JsonObject result = branchGroup.query(query);
        assertThat(result.toString()).contains("result");
    }

    private BlockHusk newBlock(List<TransactionHusk> body, BlockHusk prevBlock) {
        return new BlockHusk(TestConstants.wallet(), body, prevBlock);
    }

    private void addBranch(BlockChain blockChain) {
        branchGroup.addBranch(blockChain,
                new BranchEventListener() {
                    @Override
                    public void chainedBlock(BlockHusk block) {
                    }

                    @Override
                    public void receivedTransaction(TransactionHusk tx) {
                    }
                });
    }
}
