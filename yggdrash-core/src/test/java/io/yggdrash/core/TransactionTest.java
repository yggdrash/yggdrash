package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    TransactionBody txBody;
    TransactionHeader txHeader;
    Wallet wallet;
    TransactionSignature txSig;
    Transaction tx;

    @Before
    public void init() {

        try {
            JsonObject jsonParams1 = new JsonObject();
            jsonParams1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
            jsonParams1.addProperty("amount", "10000000");

            JsonObject jsonObject1 = new JsonObject();
            jsonObject1.addProperty("method", "transfer");
            jsonObject1.add("params", jsonParams1);

            JsonObject jsonParams2 = new JsonObject();
            jsonParams2.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2001");
            jsonParams2.addProperty("amount", "5000000");

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("method", "transfer");
            jsonObject2.add("params", jsonParams2);

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject1);
            jsonArray.add(jsonObject2);

            txBody = new TransactionBody(jsonArray);

            byte[] chain = new byte[20];
            byte[] version = new byte[8];
            byte[] type = new byte[8];
            long timestamp = TimeUtils.time();

            txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

            wallet = new Wallet();
            log.debug("wallet.pubKey=" + Hex.toHexString(wallet.getPubicKey()));

            txHeader.setTimestamp(TimeUtils.time());

            txSig = new TransactionSignature(wallet, txHeader.getHashForSignning());
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void testTransactionConstructor() {

        try {
            Transaction tx0 = new Transaction(txHeader, txSig, txBody);

            log.debug("tx0=" + tx0.toJsonObject());
            log.debug("tx0=" + tx0.toString());
            log.debug("tx0=" + tx0.toStringPretty());

            txHeader.setTimestamp(TimeUtils.time());
            Transaction tx1 = new Transaction(txHeader, wallet, txBody);
            log.debug("tx1=" + tx1.toJsonObject());
            log.debug("tx1=" + tx1.toString());
            log.debug("tx1=" + tx1.toStringPretty());

            Transaction tx2
                    = new Transaction(txHeader.clone(), tx1.getSignature().clone(), txBody.clone());
            log.debug("tx2=" + tx2.toJsonObject());
            log.debug("tx2=" + tx2.toString());
            log.debug("tx2=" + tx2.toStringPretty());

            assertEquals(tx1.toJsonObject(), tx2.toJsonObject());

            tx1.getHeader().setTimestamp(TimeUtils.time());

            assertNotEquals(tx1.toJsonObject().toString(), tx2.toJsonObject().toString());
            log.debug("tx1=" + tx1.toJsonObject());
            log.debug("tx2=" + tx2.toJsonObject());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void testTransactionClone() {
        try {
            Transaction tx1 = new Transaction(txHeader, txSig, txBody);
            log.debug("tx1=" + tx1.toJsonObject());

            Transaction tx2 = tx1.clone();
            log.debug("tx2=" + tx2.toJsonObject());

            assertEquals(tx1.toJsonObject(), tx2.toJsonObject());

            tx2.getHeader().setTimestamp(TimeUtils.time());
            log.debug("tx1=" + tx1.toJsonObject());
            log.debug("tx2=" + tx2.toJsonObject());

            assertNotEquals(tx1.toJsonObject().toString(), tx2.toJsonObject().toString());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }

    @Test
    public void testTransactionField() {
        try {
            Transaction tx1 = new Transaction(txHeader, txSig, txBody);
            log.debug("tx1=" + tx1.toJsonObject());

            Transaction tx2 = tx1.clone();
            log.debug("tx2=" + tx2.toJsonObject());

            assertEquals(txHeader.toJsonObject().toString(),
                    tx2.getHeader().toJsonObject().toString());
            assertArrayEquals(txSig.getSignature(), txSig.getSignature());
            assertEquals(txBody.getHexString(), tx2.getBody().getHexString());

            tx2.getHeader().setTimestamp(TimeUtils.time());
            log.debug("tx1=" + tx1.toJsonObject());
            log.debug("tx2=" + tx2.toJsonObject());

            assertNotEquals(tx1.toJsonObject().toString(), tx2.toJsonObject().toString());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }

    @Test
    public void testTransactionGetHash() {
        try {
            Transaction tx1 = new Transaction(txHeader, txSig, txBody);
            log.debug("tx1=" + tx1.toJsonObject());

            Transaction tx2 = tx1.clone();
            log.debug("tx2=" + tx2.toJsonObject());

            assertEquals(tx1.getHashString(), tx2.getHashString());

            tx2.getHeader().setTimestamp(TimeUtils.time());
            log.debug("tx1 hash=" + tx1.getHashString());
            log.debug("tx2 hash=" + tx2.getHashString());

            assertNotEquals(tx1.getHashString(), tx2.getHashString());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }

    @Test
    public void testTransactionKey() {
        try {
            Transaction tx1 = new Transaction(txHeader, txSig, txBody);
            log.debug("tx1 pubKey=" + tx1.getPubKeyHexString());

            Transaction tx2 = tx1.clone();
            log.debug("tx2 pubKey=" + tx2.getPubKeyHexString());

            assertEquals(tx1.getPubKeyHexString(), tx2.getPubKeyHexString());
            assertArrayEquals(tx1.getPubKey(), tx2.getPubKey());
            assertArrayEquals(tx1.getPubKey(), wallet.getPubicKey());

            log.debug("tx1 address=" + tx1.getAddressToString());
            log.debug("tx2 address=" + tx2.getAddressToString());
            log.debug("wallet address=" + wallet.getHexAddress());
            assertArrayEquals(tx1.getAddress(), tx2.getAddress());
            assertArrayEquals(tx1.getAddress(), wallet.getAddress());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }


}
