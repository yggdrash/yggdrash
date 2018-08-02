package io.yggdrash.core;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TransactionValidatorTest {

    @Test
    public void txSigValidateTest() throws IOException, InvalidCipherTextException {
        JsonObject json = new JsonObject();
        json.addProperty("id", "1234");
        json.addProperty("method", "run");
        Transaction tx = new Transaction(new Wallet(), json);

        TransactionValidator txValidator = new TransactionValidator();
        assertTrue(txValidator.txSigValidate(tx));
    }
}
