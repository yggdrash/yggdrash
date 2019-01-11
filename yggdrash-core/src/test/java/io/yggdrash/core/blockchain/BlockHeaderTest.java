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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockHeaderTest {

    private static final Logger log = LoggerFactory.getLogger(BlockHeaderTest.class);

    private final byte[] chain = new byte[20];
    private final byte[] version = new byte[8];
    private final byte[] type = new byte[8];
    private final byte[] prevBlockHash = new byte[32];
    private final long index = 0;
    private final long timestamp = TimeUtils.time();

    @Test
    public void testBlockHeader() throws Exception {

        JsonObject jsonParam1 = new JsonObject();
        jsonParam1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParam1.addProperty("amount", "10000000");

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("method", "transfer");
        jsonObject1.add("params", jsonParam1);

        JsonObject jsonParam2 = new JsonObject();
        jsonParam2.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2001");
        jsonParam2.addProperty("amount", "5000000");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("method", "transfer");
        jsonObject2.add("params", jsonParam2);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        TransactionBody txBody = new TransactionBody(jsonArray);
        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        TransactionSignature txSig =
                new TransactionSignature(TestConstants.wallet(), txHeader.getHashForSigning());

        Transaction tx1 = new Transaction(txHeader, txSig.getSignature(), txBody);
        Transaction tx2 = tx1.clone();

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        BlockBody blockBody1 = new BlockBody(txs1);
        BlockHeader blockHeader1 = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        BlockHeader blockHeader2 = blockHeader1.clone();
        assertEquals(blockHeader1.toJsonObject(), blockHeader2.toJsonObject());
        assertArrayEquals(blockHeader1.getHashForSigning(), blockHeader2.getHashForSigning());

        BlockHeader blockHeader3 = new BlockHeader(blockHeader1.toBinary());
        assertEquals(blockHeader1.toJsonObject(), blockHeader3.toJsonObject());
        assertArrayEquals(blockHeader1.getHashForSigning(), blockHeader3.getHashForSigning());
    }

}
