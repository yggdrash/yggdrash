package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.StateStore;
import io.yggdrash.core.husk.TransactionHusk;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

public class RuntimeTest {
    private final Runtime runtime = new Runtime();
    private Wallet wallet;
    private StateStore stateStore;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        this.stateStore = new StateStore();
    }

    @Test
    public void executeTest() throws Exception {

        JsonObject txObj = new JsonObject();
        txObj.addProperty("operator", "transfer");
        txObj.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj.addProperty("amount", 100);

        TransactionHusk tx = new TransactionHusk(txObj).sign(wallet);
        CoinContract coinContract = new CoinContract(stateStore);

        runtime.execute(coinContract, tx);
    }
}
