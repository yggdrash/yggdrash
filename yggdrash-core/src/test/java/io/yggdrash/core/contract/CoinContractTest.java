package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.StateStore;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CoinContractTest {

    private CoinContract coinContract;

    @Before
    public void setUp() {
        StateStore stateStore = new StateStore();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
        coinContract = new CoinContract();
        coinContract.init(stateStore, txReceiptStore);
    }

    @Test
    public void balanceTest() throws Exception {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        params.add(param);

        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", "balanceOf");
        query.add("params", params);

        JsonObject result = coinContract.query(query);
        assertThat(result).isNotNull();
    }

    @Test
    public void transferTest() throws Exception {
        Wallet wallet = new Wallet();
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
        boolean result = coinContract.invoke(tx);
        assertThat(result).isTrue();
    }

    private JsonObject query(JsonObject query) throws Exception {
        JsonObject res = coinContract.query(query);
        return res;
    }

    private Boolean invoke(TransactionHusk tx) throws Exception {
        Boolean res = coinContract.invoke(tx);
        return res;
    }
}
