package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.core.exception.DuplicatedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchGroupTest {

    private final Wallet wallet = TestUtils.wallet();

    private BranchGroup branchGroup;
    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        branchGroup = new BranchGroup();
        BlockChain blockChain = TestUtils.createBlockChain(false);
        addBranch(blockChain);
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        tx = TestUtils.createBranchTxHusk(wallet);
        block = new BlockHusk(wallet, Collections.singletonList(tx), blockChain.getPrevBlock());
    }

    @After
    public void tearDown() {
        TestUtils.clearTestDb();
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        addBranch(TestUtils.createBlockChain(false));
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
        branchGroup.generateBlock(wallet);
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

        BlockHusk newBlock = new BlockHusk(wallet, Collections.singletonList(tx), block);
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
        JsonObject query = ContractQry.createQuery(null, "getAllBranchId",
                new JsonArray());
        JsonObject resultObject = contract.query(query);
        String result = resultObject.get("result").getAsString();
        assertThat(result).contains(block.getBranchId().toString());
    }

    @Test
    public void query() {
        JsonArray params = ContractQry.createParams(
                "branchId", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject query = ContractQry.createQuery(block.getBranchId().toString(), "view", params);
        JsonObject result = branchGroup.query(query);
        assertThat(result.toString()).contains("result");
    }

    private void addBranch(BlockChain blockChain) {
        branchGroup.addBranch(blockChain.getBranchId(), blockChain,
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
