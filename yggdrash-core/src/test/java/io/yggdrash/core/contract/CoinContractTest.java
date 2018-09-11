package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CoinContractTest {

    private CoinContract coinContract;

    @Before
    public void setUp() {
        StateStore<Long> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
        coinContract = new CoinContract();
        coinContract.init(stateStore, txReceiptStore);
    }

    @Test
    public void balanceTest() throws Exception {
        JsonObject result = coinContract.query(TestUtils.sampleBalanceOfQueryJson());
        assertThat(result).isNotNull();
    }

    @Test
    public void transferTest() throws Exception {
        Wallet wallet = new Wallet();

        TransactionHusk tx = new TransactionHusk(TestUtils.sampleTxObject(wallet));
        boolean result = coinContract.invoke(tx);
        assertThat(result).isTrue();
    }

    private JsonObject query(JsonObject query) throws Exception {
        return coinContract.query(query);
    }

    private Boolean invoke(TransactionHusk tx) throws Exception {
        return coinContract.invoke(tx);
    }

}
