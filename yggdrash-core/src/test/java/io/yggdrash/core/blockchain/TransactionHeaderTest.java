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
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.ByteUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TransactionHeaderTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeaderTest.class);

    private final byte[] chain = Constants.EMPTY_BRANCH;
    private final byte[] version = Constants.EMPTY_BYTE8;
    private final byte[] type = Constants.EMPTY_BYTE8;
    private long timestamp;
    private byte[] bodyHash;
    private long bodyLength;

    private TransactionBody txBody;

    @Before
    public void init() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("test1", "01");

        timestamp = TimeUtils.time();
        txBody = new TransactionBody(jsonObject);
        bodyHash = txBody.getHash();
        bodyLength = txBody.getLength();
    }

    @Test
    public void testTransactionHeader() {

        TransactionHeader txHeader =
                new TransactionHeader(chain, version, type, Long.MAX_VALUE, bodyHash, Long.MAX_VALUE);
        assertEquals(TransactionHeader.LENGTH, txHeader.getBinaryForSigning().length);

        TransactionHeader txHeader1 =
                new TransactionHeader(chain, version, type, timestamp, bodyHash, bodyLength);

        log.debug(txHeader1.toString());
        log.debug(txHeader1.toJsonObject().toString());

        log.debug("chain=" + Hex.toHexString(txHeader1.getChain()));
        log.debug("version=" + Hex.toHexString(txHeader1.getVersion()));
        log.debug("type=" + Hex.toHexString(txHeader1.getType()));
        log.debug("timestamp="
                + Hex.toHexString(ByteUtil.longToBytes(txHeader1.getTimestamp())));
        log.debug("bodyHash="
                + Hex.toHexString(txHeader1.getBodyHash()));
        log.debug("bodyLength="
                + Hex.toHexString(ByteUtil.longToBytes(txHeader1.getBodyLength())));

        TransactionHeader txHeader2
                = new TransactionHeader(chain, version, type, timestamp, txBody);

        log.debug(txHeader2.toString());
        log.debug(txHeader2.toJsonObject().toString());

        log.debug("chain=" + Hex.toHexString(txHeader2.getChain()));
        log.debug("version=" + Hex.toHexString(txHeader2.getVersion()));
        log.debug("type=" + Hex.toHexString(txHeader2.getType()));
        log.debug("timestamp="
                + Hex.toHexString(ByteUtil.longToBytes(txHeader2.getTimestamp())));
        log.debug("bodyHash="
                + Hex.toHexString(txHeader2.getBodyHash()));
        log.debug("bodyLength="
                + Hex.toHexString(ByteUtil.longToBytes(txHeader2.getBodyLength())));

        assertEquals(txHeader1.toJsonObject(), txHeader2.toJsonObject());

        assertArrayEquals(txHeader1.getHashForSigning(), txHeader2.getHashForSigning());

        JsonObject jsonObject3 = txHeader2.toJsonObject();
        jsonObject3.addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(TimeUtils.time() + 1)));
        log.debug("jsonObject3=" + jsonObject3.toString());

        TransactionHeader txHeader3 = new TransactionHeader(jsonObject3);
        log.debug("txHeader1=" + txHeader1.toJsonObject());
        log.debug("txHeader3=" + txHeader3.toJsonObject());
        assertNotEquals(txHeader1.toJsonObject(), txHeader3.toJsonObject());


        TransactionHeader txHeader4 = new TransactionHeader(txHeader1.toJsonObject());
        log.debug("txHeader4=" + txHeader4.toJsonObject());

        assertEquals(txHeader1.toJsonObject(), txHeader4.toJsonObject());
    }
}
