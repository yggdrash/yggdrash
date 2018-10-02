package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.TimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class BlockBodyTest {

    private static final Logger log = LoggerFactory.getLogger(BlockBodyTest.class);

    private Transaction tx1;
    private Transaction tx2;

    @Before
    public void init() {

        Wallet wallet;
        TransactionBody txBody;
        TransactionHeader txHeader;
        TransactionSignature txSig;

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

            txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());

            tx1 = new Transaction(txHeader, txSig, txBody);

            tx2 = tx1.clone();

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @After
    public void exit() {

    }

    @Test
    public void testBlockBodyTest() {

        try {

            log.debug("tx1=" + tx1.toString());
            log.debug("tx2=" + tx2.toString());

            List<Transaction> txs1 = new ArrayList<>();
            txs1.add(tx1);
            txs1.add(tx2);

            log.debug("txs=" + txs1.toString());

            BlockBody bb1 = new BlockBody(txs1);
            BlockBody bb2 = bb1.clone();

            log.debug("bb1=" + bb1.toString());
            log.debug("bb2=" + bb2.toString());

            assertEquals(bb1.toString(), bb2.toString());

            log.debug("bb1.length=" + bb1.length());
            log.debug("bb2.length=" + bb2.length());

            assertEquals(bb1.length(), bb2.length());

            log.debug("bb1.getBodyCount=" + bb1.getBodyCount());
            log.debug("bb2.getBodyCount=" + bb2.getBodyCount());
            assertEquals(bb1.getBodyCount(), 2);
            assertEquals(bb1.getBodyCount(), bb2.getBodyCount());

            log.debug("bb1.merkleRoot=" + Hex.toHexString(bb1.getMerkleRoot()));
            log.debug("bb2.merkleRoot=" + Hex.toHexString(bb2.getMerkleRoot()));

            assertArrayEquals(bb1.getMerkleRoot(), bb2.getMerkleRoot());
            assertEquals(bb1.getMerkleRoot().length, 32);

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

}
