package io.yggdrash.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStreamReader;
import java.util.Scanner;

public class NodeContractDemoClient {

    private static JsonObject getSampleBranch(String path) throws Exception {
        String sampleSeedPath = "classpath:/seed/%s";
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(String.format(sampleSeedPath, path));
        JsonParser jsonParser = new JsonParser();

        return (JsonObject) jsonParser.parse(
                new InputStreamReader(resource.getInputStream(), "UTF-8"));
    }

    private static void createTx(Wallet wallet, JsonObject branch, String method) {
        String branchId = TestUtils.getBranchId(branch);
        System.out.println("BranchId >> " + branchId);

        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branch);
        params.add(param);

        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", method);
        txObj.add("params", params);

        TransactionHusk tx = new TransactionHusk(TestUtils.sampleTxObject(wallet, txObj));
        System.out.println("Transaction >> " + tx);
    }

    private static void createBranchTx(Wallet wallet, JsonObject branch) {
        createTx(wallet, branch, "create");
    }

    private static void updateBranchTx(Wallet wallet, JsonObject updatedBranch) {
        createTx(wallet, updatedBranch, "update");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        Wallet wallet = new Wallet();

        String input;
        Scanner scan = new Scanner(System.in);


        System.out.println("사용할 .json 파일명을 입력하세요 (예. /seed/sample1.json) : ");
        input = scan.nextLine();
        JsonObject branch = getSampleBranch(input);

        JsonArray versionHistory = new JsonArray();
        versionHistory.add(branch.get("version").getAsString());
        System.out.println("===[Result]===\nSeed >> " + branch);
        branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("timestamp", System.currentTimeMillis());
        branch.add("versionHistory", versionHistory);
        System.out.println("Branch >> " + branch);

        createBranchTx(wallet, branch);
    }
}
