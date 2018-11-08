package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContractApiImplTest {
    private static final ContractApi contractApi = new JsonRpcConfig().contractApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();
    private static JsonObject branch;
    private static BranchId branchId;

    @BeforeClass
    public static void beforeTest() {
        branch = TestUtils.getSampleBranch1();
        branchId = BranchId.of(branch);

        try {
            TransactionHusk tx = ContractTx.createStemTxBySeed(TestUtils.STEM,
                    TestUtils.wallet(), branch, "create");
            txApi.sendTransaction(TransactionDto.createBy(tx));
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void contractApiIsNotNull() {
        assertThat(contractApi).isNotNull();
    }

    @Test
    public void TransactionApiIsNotNull() {
        assertThat(txApi).isNotNull();
    }

    @Test
    public void update() {
        try {
            String description = "hello world!";
            String updatedVersion = "0xf4312kjise099qw0nene76555484ab1547av8b9e";
            JsonObject updatedBranch = TestUtils.updateBranch(description, updatedVersion,
                    branch, 0);

            TransactionHusk tx =
                    ContractTx.createStemTxBySeed(TestUtils.STEM, TestUtils.wallet(),
                            updatedBranch, "update");
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void search() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "search", ContractQry
                    .createParams("key", "type", "value", "immunity"));
            contractApi.query(queryObj.toString());

            queryObj = ContractQry.createQuery(branchId.toString(),
                    "search", ContractQry.createParams(
                    "key", "name", "value", "TEST1"));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void view() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "view", ContractQry.createParams(
                    "branchId", branchId.toString()));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCurrentVersion() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getcurrentversion",
                    ContractQry.createParams("branchId", branchId.toString()));

            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVersionHistory() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getversionhistory",
                    ContractQry.createParams("branchId", branchId.toString()));
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAllBranchId() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "getallbranchid",
                    new JsonArray());
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}