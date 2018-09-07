package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TransactionBodyTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionBodyTest.class);

    @Before
    public void init() {

    }

    @After
    public void exit() {

    }

    @Test
    public void testTransactionBody() {

        try {
            JsonObject jsonObject1 = new JsonObject();
            jsonObject1.addProperty("test1", "01");

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("test2", "02");

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject1);
            jsonArray.add(jsonObject2);

            log.debug("JsonObject1=" + jsonObject1.toString());
            log.debug("JsonObject1Size=" + jsonObject1.size());
            log.debug("JsonObject1StringSize=" + jsonObject1.toString().length());

            log.debug("JsonObject2=" + jsonObject2.toString());
            log.debug("JsonObject1Size=" + jsonObject2.size());
            log.debug("JsonObject1StringSize=" + jsonObject2.toString().length());

            log.debug("JsonArray=" + jsonArray.toString());
            log.debug("JsonArraySize=" + jsonArray.size());
            log.debug("JsonArrayStringSize=" + jsonArray.toString().length());

            TransactionBody txBody = new TransactionBody(jsonArray);

            log.debug("txBody=" + txBody.getBody().toString());
            assertEquals(jsonArray.toString(), txBody.getBody().toString());

            log.debug("txBody Hex String=" + txBody.toHexString());
            assertEquals(Hex.toHexString(jsonArray.toString().getBytes()), txBody.toHexString());

            log.debug("txBody length=" + txBody.length());
            assertEquals(jsonArray.toString().length(), txBody.length());

            log.debug("txBody Binary=" + Hex.toHexString(txBody.toBinary()));

            assertArrayEquals(jsonArray.toString().getBytes(), txBody.toBinary());

            log.debug("txBody count=" + txBody.getBodyCount());

            assertEquals(txBody.getBodyCount(), 2);

            TransactionBody txBody2 =  new TransactionBody(jsonArray.toString());

            assertEquals(txBody.toString(), txBody2.toString());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

}
