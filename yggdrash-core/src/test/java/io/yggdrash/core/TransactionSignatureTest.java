
package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SignatureException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TransactionSignatureTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionSignatureTest.class);

    TransactionBody txBody;
    TransactionHeader txHeader;
    Wallet wallet;

    @Before
    public void init() throws IOException, InvalidCipherTextException {

        try {
            byte[] chain = new byte[20];
            byte[] version = new byte[8];
            byte[] type = new byte[8];
            long timestamp = TimeUtils.time();

            JsonObject jsonObject1 = new JsonObject();
            jsonObject1.addProperty("test1", "01");

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("test2", "02");

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject1);
            jsonArray.add(jsonObject2);

            txBody = new TransactionBody(jsonArray);

            txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

            wallet = new Wallet();
            log.debug("wallet.pubKey=" + Hex.toHexString(wallet.getPubicKey()));
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void testTransactionSignature() {

        try {
            TransactionSignature txSig1 = new TransactionSignature(wallet, txHeader.getHeaderHashForSigning());
            assertArrayEquals(txSig1.getBodyHash(), txHeader.getHeaderHashForSigning());

            log.debug("txSig1.signature=" + Hex.toHexString(txSig1.getSignature()));
            log.debug("txSig1.data=" + Hex.toHexString(txSig1.getBodyHash()));
            log.debug("txSig1.ecKeyPub=" + Hex.toHexString(txSig1.getEcKeyPub().getPubKey()));

            TransactionSignature txSig2 = new TransactionSignature(txSig1.getSignature(), txSig1.getBodyHash());

            log.debug("txSig2.signature=" + Hex.toHexString(txSig2.getSignature()));
            log.debug("txSig2.data=" + Hex.toHexString(txSig2.getBodyHash()));
            log.debug("txSig2.ecKeyPub=" + Hex.toHexString(txSig2.getEcKeyPub().getPubKey()));

            assertArrayEquals(txSig1.getEcdsaSignature().toBinary(), txSig2.getEcdsaSignature().toBinary());

            assertArrayEquals(txSig1.getEcKeyPub().getPubKey(), txSig2.getEcKeyPub().getPubKey());
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void testTransactionSignatureClone() {
        try {
            TransactionSignature txSig1 = new TransactionSignature(wallet, txHeader.getHeaderHashForSigning());
            TransactionSignature txSig2 = txSig1.clone();
            log.debug("txSig1=" + txSig1.getSignatureHexString());
            log.debug("txSig2=" + txSig2.getSignatureHexString());

            assertArrayEquals(txSig1.getSignature(), txSig2.getSignature());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }





}
