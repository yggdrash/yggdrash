package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractQry;
import io.yggdrash.core.event.BranchEventListener;
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
    public void setUp() throws InstantiationException, IllegalAccessException {
        branchGroup = new BranchGroup();
        addBranch(TestUtils.createBlockChain(false));
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        tx = TestUtils.createBranchTxHusk(wallet);
        block = new BlockHusk(wallet, Collections.singletonList(tx),
                branchGroup.getBlockByIndex(BranchId.stem(), 0));
    }

    @After
    public void tearDown() {
        TestUtils.clearTestDb();
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() throws InstantiationException, IllegalAccessException {
        addBranch(TestUtils.createBlockChain(false));
    }

    @Test
    public void addTransaction() {
        branchGroup.addTransaction(tx);
        TransactionHusk pooledTx1 = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
        assertThat(pooledTx1.getHash()).isEqualTo(tx.getHash());
        TransactionHusk pooledTx2 = branchGroup.getTxByHash(tx.getBranchId(),
                tx.getHash().toString());
        assertThat(pooledTx2.getHash()).isEqualTo(tx.getHash());
        assertThat(branchGroup.getTransactionList(tx.getBranchId()).size()).isEqualTo(2);
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

        assertThat(branchGroup.getLastIndex(BranchId.stem())).isEqualTo(2);
        assertThat(branchGroup.getBlockByIndex(BranchId.stem(), 2).getHash())
                .isEqualTo(newBlock.getHash());
        TransactionHusk foundTx = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
        assertThat(foundTx.getHash()).isEqualTo(tx.getHash());
    }

    @Test
    public void getStateStore() {
        assertThat(branchGroup.getStateStore(BranchId.stem())).isNotNull();
    }

    @Test
    public void getTransactionReceiptStore() {
        assertThat(branchGroup.getTransactionReceiptStore(BranchId.stem())).isNotNull();
    }

    @Test
    public void getContract() {
        assertThat(branchGroup.getContract(BranchId.stem())).isNotNull();
    }

    @Test
    public void query() {
        JsonArray params = ContractQry.createParams(
                "branchId", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject query = ContractQry.createQuery(BranchId.STEM, "view", params);
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
                },
                contractEvent -> {
                });
    }
}