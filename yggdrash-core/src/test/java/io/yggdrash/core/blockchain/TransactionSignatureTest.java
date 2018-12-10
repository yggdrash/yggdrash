
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
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TransactionSignatureTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionSignatureTest.class);

    private TransactionSignature txSig1;

    @Before
    public void setUp() {
        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("test1", "01");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("test2", "02");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        TransactionBody txBody = new TransactionBody(jsonArray);

        byte[] chain = new byte[20];
        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);
        txSig1 = new TransactionSignature(TestConstants.wallet(), txHeader.getHashForSigning());
    }

    @Test
    public void testTransactionSignature() {
        TransactionSignature txSig2 = new TransactionSignature(txSig1.getSignature());

        log.debug("txSig2.signature=" + Hex.toHexString(txSig2.getSignature()));
        log.debug("txSig2.signatureHex=" + txSig2.getSignatureHexString());
        log.debug("txSig2.toJsonObject=" + txSig2.toJsonObject());
        log.debug("txSig2.toString=" + txSig2.toString());

        assertArrayEquals(txSig1.getSignature(), txSig2.getSignature());

        TransactionSignature txSig3 = new TransactionSignature(txSig1.toJsonObject());
        log.debug("txSig1=" + txSig1.toString());
        log.debug("txSig3=" + txSig3.toString());

        assertEquals(txSig1.toString(), txSig3.toString());

        TransactionSignature txSig4 = new TransactionSignature(txSig1.toJsonObject());
        assertEquals(txSig1.toString(), txSig4.toString());
    }

    @Test
    public void testTransactionSignatureClone() throws Exception {
        TransactionSignature txSig2 = txSig1.clone();
        log.debug("txSig1=" + txSig1.getSignatureHexString());
        log.debug("txSig2=" + txSig2.getSignatureHexString());

        assertArrayEquals(txSig1.getSignature(), txSig2.getSignature());
    }

}
