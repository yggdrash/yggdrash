package io.yggdrash.core;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class TransactionValidatorTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionValidatorTest.class);

    @Test
    public void txSigValidateTest() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "1234");
        json.addProperty("method", "run");
        Transaction tx = new Transaction(json);
        WalletMock.sign(tx);

        TransactionValidator txValidator = new TransactionValidator();
        assertTrue(txValidator.txSigValidate(tx));
    }
}
