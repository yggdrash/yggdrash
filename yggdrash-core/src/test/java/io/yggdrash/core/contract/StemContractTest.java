package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.contract.StemContract;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private Wallet wallet;
    private JsonObject referenceBranch;

    @Before
    public void setUp() throws Exception {
        StateStore<JsonObject> stateStore = new StateStore<JsonObject>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
        wallet = new Wallet();
        stemContract = new StemContract();
        stemContract.init(stateStore, txReceiptStore);
        String name = "TEST";
        String symbol = "TEST";
        String property = "dex";
        String type = "immunity";
        String description = "TEST";
        String version = "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress = "";
        String reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        referenceBranch = createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
        stemContract.create(referenceBranch);

        name = "TEST2";
        symbol = "TEST2";
        property = "exchange";
        type = "mutable";
        description = "TEST2";
        version = "0xe4452ervbo091qw4f5n2s8799232abr213er2c90";
        referenceAddress = "";
        reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        JsonObject referenceBranch2 = createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
        stemContract.create(referenceBranch2);
    }

    @Test
    public void createTest() {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String type = "immunity";
        String description = "ETH TO YEED";
        String version = "0xb5790adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress =
                "ce51eb47741d9e782ffa8b28743a1bb4d8af5d149cd1acd9bbbcbb6764b9c1e9";
        String reserveAddress = "0x1F8f8A219550f89f9D372ab2eE0D1f023EC665a3";
        stemContract.create(createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress));
    }

    @Test
    public void updateTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        String description = "Hello World!";
        String updatedVersion = "0xf4312kjise099qw0nene76555484ab1547av8b9e";
        JsonObject updatedBranch = updateBranch(description, updatedVersion, referenceBranch, 0);
        stemContract.update(branchId, updatedBranch);
        updatedBranch = updateBranch(description, updatedVersion, referenceBranch, 1);
        stemContract.update(branchId, updatedBranch);
    }

    @Test
    public void searchTest() {
        String key = "type";
        String element = "immunity";
        stemContract.search(key, element);

        key = "name";
        element = "TEST2";
        stemContract.search(key, element);

        key = "property";
        element = "dex";
        stemContract.search(key, element);

        key = "owner";
        element = "9e187f5264037ab77c87fcffcecd943702cd72c3";
        stemContract.search(key, element);

        key = "symbol";
        element = "TEST2";
        stemContract.search(key, element);
    }

    @Test
    public void viewTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        stemContract.view(branchId);
    }

    @Test
    public void getCurrentVersionTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        log.debug(stemContract.getCurrentVersion(branchId));
    }

    @Test
    public void getVersionHistoryTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        log.debug(stemContract.getVersionHistory(branchId).getAsString());
    }

    private JsonObject createBranch(String name,
                                    String symbol,
                                    String property,
                                    String type,
                                    String description,
                                    String version,
                                    String referenceAddress,
                                    String reserveAddress) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(version);
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("from", "f4a3760644d064b3f7d82bb8e43ccb090a2dac8b55cc2894bf618c551b0bc2a8");
        branch.addProperty("to", "83d2834d74afcba266fb3305bd2ff4a3cf35fe9c455e288975ea39b68e255156");
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("timestamp", "0000016531dfa31c");
        branch.addProperty("description", description);
        branch.addProperty("ratio", 0.1);
        branch.addProperty("version", version);
        branch.add("versionHistory", versionHistory);
        branch.addProperty("reference_address", referenceAddress);
        branch.addProperty("reserve_address", reserveAddress);

        return branch;
    }

    private JsonObject updateBranch(String description, String version, JsonObject branch, Integer checkSum) {
        JsonObject updatedBranch = new JsonObject();
        updatedBranch.addProperty("name", checkSum == 0 ? branch.get("name").getAsString() : "HELLO");
        updatedBranch.addProperty("owner", branch.get("owner").getAsString());
        updatedBranch.addProperty("from", branch.get("from").getAsString());
        updatedBranch.addProperty("to", branch.get("to").getAsString());
        updatedBranch.addProperty("symbol", branch.get("symbol").getAsString());
        updatedBranch.addProperty("property", branch.get("property").getAsString());
        updatedBranch.addProperty("type", branch.get("type").getAsString());
        updatedBranch.addProperty("timestamp", branch.get("timestamp").getAsString());
        updatedBranch.addProperty("description", description);
        updatedBranch.addProperty("ration", branch.get("ratio").getAsFloat());
        updatedBranch.addProperty("version", version);
        updatedBranch.add("versionHistory", branch.get("versionHistory").getAsJsonArray());
        updatedBranch.addProperty("reference_address", branch.get("reference_address").getAsString());
        updatedBranch.addProperty("reserve_address", branch.get("reserve_address").getAsString());

        return updatedBranch;
    }
}