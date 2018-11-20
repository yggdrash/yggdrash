package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.contract.ContractQry;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ContractApiImplTest {
    private static final ContractApi contractApi = new JsonRpcConfig().contractApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();
    private static JsonObject branch;
    private static BranchId branchId;

    @Before
    public void setUp() {
        boolean isStemTest = false;

        if (isStemTest) {
            beforeStemTest();
        } else {
            branchId = BranchId.of("96550eca8544ac0b91365697b05b607ed785f0bb");
        }
    }

    static void beforeStemTest() {
        branch = TestUtils.getSampleBranch();
        branchId = BranchId.of(branch);

        try {
            TransactionHusk tx = ContractTx.createStemTx(TestUtils.wallet(), branch, "create");
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


    /* StemContract Test */
    @Test
    public void update() {
        try {
            String description = "hello world!";
            JsonObject updatedBranch = TestUtils.updateBranch(description, branch, 0);

            TransactionHusk tx =
                    ContractTx.createStemTx(TestUtils.wallet(), updatedBranch, "update");
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

    /* CoinStandardContract Test */
    @Test
    public void totalSupply() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "totalSupply", new JsonArray());
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, new BigDecimal("1000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void balanceOf() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "balanceOf", ContractQry.createParams(
                            "address", "cee3d4755e47055b530deeba062c5bd0c17eb00f"));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, new BigDecimal("998000000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void allowance() {
        try {
            JsonObject queryObj = ContractQry.createQuery(branchId.toString(),
                    "allowance", ContractQry.createParams(
                            "owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f",
                            "spender", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e"));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(
                    contractApi.query(queryObj.toString()));
            BigDecimal result = new BigDecimal(jsonObject.get("result").getAsString());

            assertEquals(result, BigDecimal.ZERO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void transfer() {
        try {
            JsonArray params = ContractTx.createTransferBody(
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createCoinContractTx(branchId, TestUtils.wallet(), params);

            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void approve() {
        try {
            JsonArray params = ContractTx.createApproveBody(
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e", new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createCoinContractTx(branchId, TestUtils.wallet(), params);
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void transferFrom() {
        try {
            JsonArray params = ContractTx.createTransferFromBody(
                    "cee3d4755e47055b530deeba062c5bd0c17eb00f",
                    "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                    new BigDecimal("1000"));
            TransactionHusk tx =
                    ContractTx.createCoinContractTx(branchId, TestUtils.wallet(), params);
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}