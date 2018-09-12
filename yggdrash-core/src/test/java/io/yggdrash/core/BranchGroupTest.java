package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.exception.DuplicatedException;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class BranchGroupTest {

    private static final Wallet wallet;
    private static final File branchJson;

    private BranchGroup branchGroup;
    private BlockChain blockChain;
    private TransactionHusk tx;
    private BlockHusk block;

    static {
        try {
            wallet = new Wallet();
            branchJson = new File(Objects.requireNonNull(BranchGroupTest.class.getClassLoader()
                    .getResource("branch-sample.json")).getFile());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    @Before
    public void setUp() {
        branchGroup = new BranchGroup();
        blockChain = new BlockChain(branchJson);
        branchGroup.addBranch(blockChain.getBranchId(), blockChain);
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        tx = TestUtils.createTxHusk(wallet);
        block = new BlockHusk(wallet, Collections.singletonList(tx),
                branchGroup.getBlockByIndex(0));
    }

    @After
    public void tearDown() {
        TestUtils.clearTestDb();
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        branchGroup.getBranch(blockChain.getBranchId()).close();
        BlockChain blockChain = new BlockChain(branchJson);
        branchGroup.addBranch(blockChain.getBranchId(), blockChain);
    }

    @Test
    public void addTransaction() {
        branchGroup.addTransaction(tx);
        TransactionHusk pooledTx1 = branchGroup.getTxByHash(tx.getHash());
        assertThat(pooledTx1.getHash()).isEqualTo(tx.getHash());
        TransactionHusk pooledTx2 = branchGroup.getTxByHash(tx.getHash().toString());
        assertThat(pooledTx2.getHash()).isEqualTo(tx.getHash());
        assertThat(branchGroup.getTransactionList().size()).isEqualTo(1);
    }

    @Test
    public void generateBlock() {
        branchGroup.addTransaction(tx);
        BlockHusk chainedBlock = branchGroup.generateBlock(wallet);
        assertThat(branchGroup.getLastIndex()).isEqualTo(1);
        assertThat(chainedBlock.getBody().size()).isEqualTo(1);
        assertThat(branchGroup.getTxByHash(tx.getHash()).getHash(), equalTo(tx.getHash()));
    }

    @Test
    public void addBlock() {
        branchGroup.addTransaction(tx);
        branchGroup.addBlock(block);

        BlockHusk newBlock = new BlockHusk(wallet, Collections.singletonList(tx), block);
        branchGroup.addBlock(newBlock);

        assertThat(branchGroup.getLastIndex()).isEqualTo(2);
        assertThat(branchGroup.getBlockByIndex(2).getHash()).isEqualTo(newBlock.getHash());
        TransactionHusk foundTx = branchGroup.getTxByHash(tx.getHash());
        assertThat(foundTx.getHash()).isEqualTo(tx.getHash());
    }

    @Test
    public void getStateStore() {
        assertThat(branchGroup.getStateStore()).isNotNull();
    }

    @Test
    public void getTransactionReceiptStore() {
        assertThat(branchGroup.getTransactionReceiptStore()).isNotNull();
    }

    @Test
    public void getContract() {
        assertThat(branchGroup.getContract()).isNotNull();
    }

    @Test
    public void query() {
        JsonObject result = branchGroup.query(TestUtils.sampleBalanceOfQueryJson());
        assertThat(result.toString()).isEqualTo("{}");
    }
}