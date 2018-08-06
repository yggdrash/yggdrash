package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
import org.junit.Test;

public class RuntimeTest {
    private final Runtime runtime = new Runtime();

    @Test
    public void executeTest() throws Exception {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("operator", "transfer");
        txObj.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj.addProperty("amount", 100);

        Transaction tx = new Transaction(txObj);
        WalletMock.sign(tx);
        CoinContract coinContract = new CoinContract();

        runtime.execute(coinContract, tx);
    }
}
