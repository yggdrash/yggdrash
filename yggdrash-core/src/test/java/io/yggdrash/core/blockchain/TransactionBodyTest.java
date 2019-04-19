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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.SerializationUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TransactionBodyTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionBodyTest.class);

    @Test
    public void testTransactionBody() {

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

        log.debug("txBody length=" + txBody.length());
        assertEquals(jsonArray.toString().length(), txBody.length());

        log.debug("txBody Binary=" + Hex.toHexString(txBody.toBinary()));

        assertArrayEquals(jsonArray.toString().getBytes(), txBody.toBinary());

        log.debug("txBody count=" + txBody.getBodyCount());

        assertEquals(2, txBody.getBodyCount());

        TransactionBody txBody2 = new TransactionBody(jsonArray);

        assertEquals(txBody, txBody2);

        TransactionBody txBody3 = new TransactionBody(jsonArray.toString().getBytes());
        log.debug("txBody1 Hex String=" + txBody.toString());
        log.debug("txBody3 Hex String=" + txBody3.toString());
        assertEquals(txBody.toString(), txBody3.toString());

        byte[] data = SerializationUtil.serializeString(jsonArray.toString());
        TransactionBody txBody4 = new TransactionBody(data);
        log.debug("txBody1 Hex String=" + txBody.toString());
        log.debug("txBody4 Hex String=" + txBody3.toString());
        assertEquals(txBody.toString(), txBody4.toString());
    }

}
