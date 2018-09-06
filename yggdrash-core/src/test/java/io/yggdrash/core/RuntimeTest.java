package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.StemContract;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeTest {
    private final TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
    private final CoinContract coinContract = new CoinContract();
    private final StemContract stemContract = new StemContract();
    private Runtime runtime;
    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        runtime = new Runtime(new StateStore(), txReceiptStore);
        wallet = new Wallet();
    }

    @Test
    public void invokeTest() throws Exception {
        runtime.invoke(coinContract, new TransactionHusk(TestUtils.sampleTx()));
    }

    @Test
    public void queryTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        params.add(param);

        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", "balanceOf");
        query.add("params", params);

        JsonObject result = runtime.query(coinContract, query);
        assertThat(result).isNotNull();
    }


    @Test
    public void invokeToYeedTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject param2 = new JsonObject();
        param2.addProperty("amount", 100);
        params.add(param1);
        params.add(param2);

        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", "transfer");
        txObj.add("params", params);

        TransactionHusk tx = new TransactionHusk(TestUtils.sampleTxObject(null, txObj));
        runtime.invoke(coinContract, tx);
    }

    @Test
    public void queryToYeedTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        params.add(param);

        JsonObject result = runtime.query(coinContract, createQuery("balanceOf", params));
        assertThat(result).isNotNull();
    }

    @Test
    public void queryToStemTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId",
                "e1bbdf827bb44f0ae1d88f34e5f3a360484adbf2cf65a6d34162af3bbd4b9523");
        params.add(param);

        JsonObject result = runtime.query(stemContract, createQuery("view", params));
        assertThat(result).isNotNull();

        result = runtime.query(stemContract, createQuery("getCurrentVersion", params));
        assertThat(result).isNotNull();

        result = runtime.query(stemContract, createQuery("getVersionHistory", params));
        assertThat(result).isNotNull();

        param.remove("branchId");
        param.addProperty("type", "immutable");
        params.remove(0);
        params.add(param);

        result = runtime.query(stemContract, createQuery("search", params));
        assertThat(result).isNotNull();
    }

    private JsonObject createQuery(String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", method);
        query.add("params", params);
        System.out.println("createQuery :: query => " + query);
        return query;
    }
}
