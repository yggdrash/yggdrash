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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private JsonObject referenceBranch;
    private Wallet wallet;

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
                "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        String reserveAddress = "0x1F8f8A219550f89f9D372ab2eE0D1f023EC665a3";
        assertThat(stemContract.create(createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress))).isNotNull();
    }

    @Test
    public void updateTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        String description = "Hello World!";
        String updatedVersion = "0xf4312kjise099qw0nene76555484ab1547av8b9e";
        JsonObject updatedBranch = updateBranch(description, updatedVersion, referenceBranch, 0);
        String res = stemContract.update(branchId, updatedBranch);
        assertEquals(res, branchId);
        updatedBranch = updateBranch(description, updatedVersion, referenceBranch, 1);
        res = stemContract.update(branchId, updatedBranch);
        assertNotEquals(res, branchId);
    }

    /*@Test
    public void searchTest() {
        String key = "type";
        String element = "immunity";
        assertThat(stemContract.search(key, element).size()).isNotZero();

        key = "name";
        element = "TEST2";
        assertThat(stemContract.search(key, element).size()).isNotZero();

        key = "property";
        element = "dex";
        assertThat(stemContract.search(key, element).size()).isNotZero();

        key = "owner";
        element = "9e187f5264037ab77c87fcffcecd943702cd72c3";
        assertThat(stemContract.search(key, element).size()).isNotZero();

        key = "symbol";
        element = "TEST2";
        assertThat(stemContract.search(key, element).size()).isNotZero();

        key = "tag";
        element = "0.1";
        assertThat(stemContract.search(key, element).size()).isNotZero();
    }*/

    /*@Test
    public void viewTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        assertThat(stemContract.view(branchId)).isNotEmpty();
    }*/

    /*@Test
    public void getCurrentVersionTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        log.debug(stemContract.getCurrentVersion(branchId));
        assertThat(stemContract.getCurrentVersion(branchId)).isNotEmpty();
    }*/

    /*@Test
    public void getVersionHistoryTest() {
        String branchId = "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523";
        log.debug(stemContract.getVersionHistory(branchId).getAsString());
        assertThat(stemContract.getVersionHistory(branchId).size()).isNotZero();
    }*/

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
        //branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("owner", "9e187f5264037ab77c87fcffcecd943702cd72c3");
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("timestamp", "0000016531dfa31c");
        branch.addProperty("description", description);
        branch.addProperty("tag", 0.1);
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
        updatedBranch.addProperty("symbol", branch.get("symbol").getAsString());
        updatedBranch.addProperty("property", branch.get("property").getAsString());
        updatedBranch.addProperty("type", branch.get("type").getAsString());
        updatedBranch.addProperty("timestamp", branch.get("timestamp").getAsString());
        updatedBranch.addProperty("description", description);
        updatedBranch.addProperty("tag", branch.get("tag").getAsFloat());
        updatedBranch.addProperty("version", version);
        updatedBranch.add("versionHistory", branch.get("versionHistory").getAsJsonArray());
        updatedBranch.addProperty("reference_address", branch.get("reference_address").getAsString());
        updatedBranch.addProperty("reserve_address", branch.get("reserve_address").getAsString());

        return updatedBranch;
    }
}