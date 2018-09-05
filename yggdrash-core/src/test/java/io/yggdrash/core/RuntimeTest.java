package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
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
    private Runtime runtime;
    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        runtime = new Runtime(new StateStore(), txReceiptStore);
        wallet = new Wallet();
    }

    @Test
    public void invokeTest() throws Exception {
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

        TransactionHusk tx = new TransactionHusk(txObj).sign(wallet);
        runtime.invoke(coinContract, tx);
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
}
