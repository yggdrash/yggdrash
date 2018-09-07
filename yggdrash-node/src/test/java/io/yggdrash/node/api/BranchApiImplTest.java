package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.Runtime;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.node.TestUtils;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(BranchApiImplTest.class);
    private static final BranchApi branchApi = new JsonRpcConfig().branchApi();
    private static final Runtime runtime =
            new Runtime(new StateStore(), new TransactionReceiptStore());
    private static final Map<String, JsonObject> branchStoreMock = new HashMap<>();
    private static Wallet wallet;
    private static JsonObject branch;
    private static String branchId;

    @BeforeClass
    public static void beforeTest() throws Exception {
        wallet = new Wallet();
        create();
    }

    @Test
    public void branchApiIsNotNull() {
        assertThat(branchApi).isNotNull();
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
            System.out.println("tx : " + tx);
            branchApi.createBranch(TransactionDto.createBy(tx));
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
            branchApi.updateBranch(TransactionDto.createBy(tx));
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
            branchApi.searchBranch(queryObj.toString());

            params.remove(0);
            param.addProperty("key", "name");
            param.addProperty("value", "TEST1");
            params.add(param);

            queryObj = TestUtils.createQuery("search", params);
            branchApi.searchBranch(queryObj.toString());
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
            branchApi.viewBranch(queryObj.toString());
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
            branchApi.getCurrentVersionOfBranch(queryObj.toString());
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
            branchApi.getVersionHistoryOfBranch(queryObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}