/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TransactionBodyTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionBodyTest.class);

    @Test
    public void testTransactionBody() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("test", "00");

        log.debug("JsonObject=" + jsonObject.toString());
        log.debug("JsonObjectSize=" + jsonObject.size());
        log.debug("JsonObjectStringSize=" + jsonObject.toString().length());

        TransactionBody txBody = new TransactionBody(jsonObject);

        log.debug("txBody=" + txBody.getBody().toString());
        assertEquals(jsonObject.toString(), txBody.getBody().toString());

        log.debug("txBody Hex String=" + txBody.toHexString());
        assertEquals(Hex.toHexString(jsonObject.toString().getBytes()), txBody.toHexString());

        log.debug("txBody length=" + txBody.length());
        assertEquals(jsonObject.toString().length(), txBody.length());

        log.debug("txBody Binary=" + Hex.toHexString(txBody.toBinary()));

        assertArrayEquals(jsonObject.toString().getBytes(), txBody.toBinary());

        log.debug("txBody count=" + txBody.getBodyCount());

        assertEquals(1, txBody.getBodyCount());

        TransactionBody txBody2 = new TransactionBody(jsonObject.toString());
        log.debug("txBody1 Hex String=" + txBody.toHexString());
        log.debug("txBody2 Hex String=" + txBody2.toHexString());

        assertEquals(txBody.toString(), txBody2.toString());

        TransactionBody txBody3 = new TransactionBody(jsonObject.toString().getBytes());
        log.debug("txBody1 Hex String=" + txBody.toString());
        log.debug("txBody3 Hex String=" + txBody3.toString());
        assertEquals(txBody.toString(), txBody3.toString());

        TransactionBody txBody4
                = new TransactionBody(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        log.debug("txBody1 Hex String=" + txBody.toString());
        log.debug("txBody4 Hex String=" + txBody3.toString());
        assertEquals(txBody.toString(), txBody4.toString());
    }

}
