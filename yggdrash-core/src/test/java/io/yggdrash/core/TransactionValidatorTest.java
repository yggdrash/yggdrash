package io.yggdrash.core;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.security.SignatureException;

import static org.junit.Assert.assertTrue;

public class TransactionValidatorTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionValidatorTest.class);

    @Test
    public void txSigValidateTest()
            throws IOException,InvalidCipherTextException,SignatureException {
        Wallet wallet = new Wallet();
        JsonObject json = new JsonObject();
        json.addProperty("id", "1234");
        json.addProperty("method", "run");
        Transaction tx = new Transaction(wallet, json);

        TransactionValidator txValidator = new TransactionValidator();
        assertTrue(txValidator.txSigValidate(tx));
    }
}
