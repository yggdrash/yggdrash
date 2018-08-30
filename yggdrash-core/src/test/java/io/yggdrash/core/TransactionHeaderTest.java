package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionHeaderTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeaderTest.class);

    byte[] chain = Hex.decode("0000000000000000000000000000000000000000");
    byte[] version = new byte[8];
    byte[] type = new byte[8];
    long timestamp;
    byte[] bodyHash;
    long bodyLength;

    TransactionBody txBody;


    @Before
    public void init() {

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("test1", "01");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("test2", "02");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        txBody = new TransactionBody(jsonArray);
        bodyHash = txBody.getBodyHash();
        bodyLength = txBody.length();

    }

    @After
    public void exit() {

    }

    @Test
    public void testTransactionHeader() {

        try {
            timestamp = TimeUtils.time();
            TransactionHeader txHeader1 = new TransactionHeader(chain, version, type, timestamp, bodyHash, bodyLength);

            log.debug(txHeader1.toString());
            log.debug(txHeader1.toJsonObject().toString());

            log.debug("chain=" + Hex.toHexString(txHeader1.getChain()));
            log.debug("version=" + Hex.toHexString(txHeader1.getVersion()));
            log.debug("type=" + Hex.toHexString(txHeader1.getType()));
            log.debug("timestamp=" + Hex.toHexString(ByteUtil.longToBytes(txHeader1.getTimestamp())));
            log.debug("bodyHash=" + Hex.toHexString(txHeader1.getBodyHash()));
            log.debug("bodyLength=" + Hex.toHexString(ByteUtil.longToBytes(txHeader1.getBodyLength())));

            TransactionHeader txHeader2 = new TransactionHeader(chain, version, type, timestamp, txBody);

            log.debug(txHeader2.toString());
            log.debug(txHeader2.toJsonObject().toString());

            log.debug("chain=" + Hex.toHexString(txHeader2.getChain()));
            log.debug("version=" + Hex.toHexString(txHeader2.getVersion()));
            log.debug("type=" + Hex.toHexString(txHeader2.getType()));
            log.debug("timestamp=" + Hex.toHexString(ByteUtil.longToBytes(txHeader2.getTimestamp())));
            log.debug("bodyHash=" + Hex.toHexString(txHeader2.getBodyHash()));
            log.debug("bodyLength=" + Hex.toHexString(ByteUtil.longToBytes(txHeader2.getBodyLength())));

            assertEquals(txHeader1.toJsonObject(), txHeader2.toJsonObject());

            assertArrayEquals(txHeader1.getHeaderHashForSigning(), txHeader2.getHeaderHashForSigning());

            TransactionHeader txHeader3 = txHeader1.clone();
            log.debug("txHeader1=" + txHeader1.toJsonObject());
            log.debug("txHeader3=" + txHeader3.toJsonObject());
            assertEquals(txHeader1.toJsonObject(), txHeader3.toJsonObject());

            txHeader3.setTimestamp(TimeUtils.time());
            log.debug("txHeader1=" + txHeader1.toJsonObject());
            log.debug("txHeader3=" + txHeader3.toJsonObject());
            assertNotEquals(txHeader1.toJsonObject(), txHeader3.toJsonObject());
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }

    @Test
    public void testTransactionHeaderClone() {
        try {
            timestamp = TimeUtils.time();
            TransactionHeader txHeader1 = new TransactionHeader(chain, version, type, timestamp, bodyHash, bodyLength);

            TransactionHeader txHeader3 = txHeader1.clone();
            log.debug("txHeader1=" + txHeader1.toJsonObject());
            log.debug("txHeader3=" + txHeader3.toJsonObject());
            assertEquals(txHeader1.toJsonObject(), txHeader3.toJsonObject());

            txHeader3.setTimestamp(TimeUtils.time());
            log.debug("txHeader1=" + txHeader1.toJsonObject());
            log.debug("txHeader3=" + txHeader3.toJsonObject());
            assertNotEquals(txHeader1.toJsonObject(), txHeader3.toJsonObject());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }
}
