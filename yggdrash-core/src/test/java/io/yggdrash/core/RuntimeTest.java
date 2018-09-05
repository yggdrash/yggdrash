package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.CoinContract;
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
        runtime = new Runtime(txReceiptStore);
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
}
