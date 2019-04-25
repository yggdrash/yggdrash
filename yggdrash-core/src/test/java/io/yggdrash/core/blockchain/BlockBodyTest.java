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
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.TimeUtils;
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

    @Test
    public void testBlockBodyTest() {

        JsonObject jsonParam = new JsonObject();
        jsonParam.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParam.addProperty("amount", "10000000");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", "transfer");
        jsonObject.add("params", jsonParam);

        byte[] chain = Constants.EMPTY_BRANCH;
        byte[] version = Constants.EMPTY_BYTE8;
        byte[] type = Constants.EMPTY_BYTE8;
        long timestamp = TimeUtils.time();

        TransactionBody txBody = new TransactionBody(jsonObject);
        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        TransactionSignature txSig =
                new TransactionSignature(TestConstants.wallet(), txHeader.getHashForSigning());

        Transaction tx1 = new TransactionImpl(txHeader, txSig.getSignature(), txBody);
        Transaction tx2 = new TransactionImpl(tx1.toBinary());

        log.debug("tx1={}", tx1);
        log.debug("tx2={}", tx2);

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs={}", txs1);

        BlockBody bb1 = new BlockBody(txs1);
        BlockBody bb2 = new BlockBody(bb1.toBinary());

        log.debug("bb1={}", bb1);
        log.debug("bb2={}", bb2);

        assertEquals(2, bb1.getCount());
        assertEquals(bb1, bb2);

        log.debug("bb1.length={}", bb1.getLength());
        log.debug("bb2.length={}", bb2.getLength());

        assertEquals(bb1.getLength(), bb2.getLength());

        log.debug("bb1.getBodyCount={}", bb1.getCount());
        log.debug("bb2.getBodyCount={}", bb2.getCount());
        assertEquals(bb1.getCount(), bb2.getCount());

        log.debug("bb1.merkleRoot={}", Hex.toHexString(bb1.getMerkleRoot()));
        log.debug("bb2.merkleRoot={}", Hex.toHexString(bb2.getMerkleRoot()));

        assertArrayEquals(bb1.getMerkleRoot(), bb2.getMerkleRoot());
        assertEquals(32, bb1.getMerkleRoot().length);
    }

}
