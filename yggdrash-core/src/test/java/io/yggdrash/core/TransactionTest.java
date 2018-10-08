package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    private TransactionBody txBody;
    private TransactionHeader txHeader;
    private Wallet wallet;
    private TransactionSignature txSig;
    private Transaction tx1;

    @Before
    public void setUp() throws Exception {

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

        txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
        tx1 = new Transaction(txHeader, txSig.getSignature(), txBody);
        assertTrue(tx1.verify());
    }

    @Test
    public void testTransactionConstructor() throws Exception {
        Transaction tx2 = new Transaction(tx1.toJsonObject());
        assertTrue(tx2.verify());
        log.debug("tx2=" + tx2.toJsonObject());
        log.debug("tx2=" + tx2.toString());
        assertEquals(tx1.toJsonObject(), tx2.toJsonObject());

        Transaction tx3
                = new Transaction(txHeader.clone(), tx1.getSignature().clone(), txBody.clone());
        assertTrue(tx3.verify());

        log.debug("tx3=" + tx3.toJsonObject());
        log.debug("tx3=" + tx3.toString());
        assertEquals(tx1.toJsonObject(), tx3.toJsonObject());

        JsonObject jsonObject = tx1.toJsonObject();
        jsonObject.getAsJsonObject("header").addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));

        Transaction tx4 = new Transaction(jsonObject);
        assertTrue(tx4.verify());

        log.debug("tx1=" + tx1.toJsonObject());
        log.debug("tx4=" + tx4.toJsonObject());
        assertNotEquals(tx1.toJsonObject().toString(), tx4.toJsonObject().toString());

        Transaction tx5 = new Transaction(tx1.getHeader(), tx1.getSignature(), tx1.getBody());
        assertTrue(tx5.verify());

        log.debug("tx1=" + tx1.toString());
        log.debug("tx5=" + tx5.toString());
        assertEquals(tx1.toJsonObject(), tx5.toJsonObject());

        Transaction tx6 = new Transaction(tx1.getHeader(), wallet, tx1.getBody());
        assertTrue(tx6.verify());

        log.debug("tx1=" + tx1.toString());
        log.debug("tx6=" + tx6.toString());
        assertEquals(tx1.toJsonObject(), tx6.toJsonObject());

        Transaction tx7 = new Transaction(tx1.toBinary());
        assertTrue(tx7.verify());

        log.debug("tx1=" + tx1.toString());
        log.debug("tx7=" + tx7.toString());
        assertEquals(tx1.toJsonObject(), tx7.toJsonObject());
    }

    @Test
    public void testTransactionClone() throws Exception {
        Transaction tx2 = tx1.clone();
        log.debug("tx2=" + tx2.toJsonObject());

        assertEquals(tx1.toJsonObject(), tx2.toJsonObject());

        JsonObject jsonObject = tx1.toJsonObject();
        jsonObject.getAsJsonObject("header").addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));

        Transaction tx3 = new Transaction(jsonObject);
        log.debug("tx1=" + tx1.toJsonObject());
        log.debug("tx3=" + tx3.toJsonObject());

        assertNotEquals(tx1.toJsonObject().toString(), tx3.toJsonObject().toString());
    }

    @Test
    public void testTransactionField() throws Exception {
        Transaction tx2 = tx1.clone();
        log.debug("tx2=" + tx2.toJsonObject());

        assertEquals(txHeader.toJsonObject().toString(),
                tx2.getHeader().toJsonObject().toString());
        assertArrayEquals(txSig.getSignature(), txSig.getSignature());
        assertEquals(txBody.toHexString(), tx2.getBody().toHexString());
    }

    @Test
    public void testTransactionGetHash() throws Exception {
        Transaction tx2 = tx1.clone();
        log.debug("tx2=" + tx2.toJsonObject());

        assertEquals(tx1.getHashString(), tx2.getHashString());

        JsonObject jsonObject = tx1.toJsonObject();
        jsonObject.getAsJsonObject("header").addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));

        Transaction tx3 = new Transaction(jsonObject);
        log.debug("tx1 hash=" + tx1.getHashString());
        log.debug("tx3 hash=" + tx3.getHashString());
        assertNotEquals(tx1.getHashString(), tx3.getHashString());
    }

    @Test
    public void testTransactionKey() throws Exception {
        Transaction tx2 = tx1.clone();
        log.debug("tx2 pubKey=" + tx2.getPubKeyHexString());
        log.debug("tx2 headerHash=" + Hex.toHexString(tx2.getHeader().getHashForSigning()));
        log.debug("tx2 pubKey=" + Hex.toHexString(tx2.getPubKey()));

        assertEquals(tx1.getPubKeyHexString(), tx2.getPubKeyHexString());
        assertArrayEquals(tx1.getPubKey(), tx2.getPubKey());
        assertArrayEquals(tx1.getPubKey(), wallet.getPubicKey());

        log.debug("tx1 address=" + tx1.getAddressToString());
        log.debug("tx2 address=" + tx2.getAddressToString());
        log.debug("wallet address=" + wallet.getHexAddress());
        log.debug("wallet signature=" + Hex.toHexString(
                wallet.signHashedData(tx1.getHeader().getHashForSigning())));
        log.debug("wallet pubKey=" + Hex.toHexString(
                wallet.getPubicKey()));

        assertArrayEquals(tx1.getAddress(), tx2.getAddress());
        assertArrayEquals(tx1.getAddress(), wallet.getAddress());
    }

    @Test
    public void testTransactionToProto() throws Exception {
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

        Proto.Transaction protoTx1 = Transaction.toProtoTransaction(tx1);
        Proto.Transaction protoTx2 = Transaction.toProtoTransaction(tx2);
        log.debug("tx1 proto=" + Hex.toHexString(protoTx1.toByteArray()));
        log.debug("tx2 proto=" + Hex.toHexString(protoTx2.toByteArray()));

        assertArrayEquals(protoTx1.toByteArray(), protoTx2.toByteArray());

        Transaction tx3 = Transaction.toTransaction(protoTx1);
        log.debug("tx1=" + tx1.toString());
        log.debug("tx3=" + tx3.toString());

        assertEquals(tx1.toString(), tx3.toString());

        Proto.Transaction protoTx3 = Transaction.toProtoTransaction(tx1);
        assertArrayEquals(protoTx1.toByteArray(), protoTx3.toByteArray());
    }

}
