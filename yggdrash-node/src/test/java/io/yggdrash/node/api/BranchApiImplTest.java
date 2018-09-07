package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.TestUtils;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(BranchApiImplTest.class);
    private static final ContractApi contractApi = new JsonRpcConfig().contractApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();
    private static Wallet wallet;
    private static JsonObject branch;
    private static String branchId;

    @BeforeClass
    public static void beforeTest() throws Exception {
        wallet = new Wallet();
        create();
    }

    @Test
    public void contractApiIsNotNull() {
        assertThat(contractApi).isNotNull();
    }

    @Test
    public void TransactionApiIsNotNull() {
        assertThat(txApi).isNotNull();
    }

    private static void create() {
        try {
            branch = TestUtils.getSampleBranch1();
            branchId = TestUtils.getBranchId(branch);
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("branchId", branchId);
            param.add("branch", branch);
            params.add(param);

            JsonObject txObj = new JsonObject();
            txObj.addProperty("method", "create");
            txObj.add("params", params);

            TransactionHusk tx = new TransactionHusk(TestUtils.sampleTxObject(wallet, txObj));
            txApi.sendTransaction(TransactionDto.createBy(tx));
            Thread.sleep(10000);
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    @Test
    public void update() {
        try {
            String description = "hello world!";
            String updatedVersion = "0xf4312kjise099qw0nene76555484ab1547av8b9e";
            JsonObject updatedBranch = TestUtils.updateBranch(description, updatedVersion,
                    branch, 0);

            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("branchId", branchId);
            param.add("branch", updatedBranch);
            params.add(param);

            JsonObject txObj = new JsonObject();
            txObj.addProperty("method", "update");
            txObj.add("params", params);

            TransactionHusk tx =  new TransactionHusk(TestUtils.sampleTxObject(wallet, txObj));
            txApi.sendTransaction(TransactionDto.createBy(tx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void search() {
        try {
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("key", "type");
            param.addProperty("value", "immunity");
            params.add(param);

            JsonObject queryObj = TestUtils.createQuery("search", params);
            contractApi.query(queryObj.toString());

            params.remove(0);
            param.addProperty("key", "name");
            param.addProperty("value", "TEST1");
            params.add(param);

            queryObj = TestUtils.createQuery("search", params);
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void view() {
        try {
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("branchId", branchId);
            params.add(param);

            JsonObject queryObj = TestUtils.createQuery("view", params);
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCurrentVersion() {
        try {
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("branchId", branchId);
            params.add(param);

            JsonObject queryObj = TestUtils.createQuery("getcurrentversion", params);
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVersionHistory() {
        try {
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("branchId", branchId);
            params.add(param);

            JsonObject queryObj = TestUtils.createQuery("getversionhistory", params);
            contractApi.query(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}