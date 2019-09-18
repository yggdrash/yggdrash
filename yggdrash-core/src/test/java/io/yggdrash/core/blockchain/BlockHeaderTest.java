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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockHeaderTest {

    private static final Logger log = LoggerFactory.getLogger(BlockHeaderTest.class);

    private final byte[] chain = Constants.EMPTY_BRANCH;
    private final byte[] version = Constants.EMPTY_BYTE8;
    private final byte[] type = Constants.EMPTY_BYTE8;
    private final byte[] prevBlockHash = Constants.EMPTY_HASH;
    private final long index = 0;
    private final long timestamp = TimeUtils.time();

    @Test
    public void testBlockHeader() {

        JsonObject jsonParam = new JsonObject();
        jsonParam.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParam.addProperty("amount", "10000000");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", "transfer");
        jsonObject.add("params", jsonParam);

        TransactionBody txBody = new TransactionBody(jsonObject);

        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        TransactionSignature txSig =
                new TransactionSignature(TestConstants.wallet(), txHeader.getHashForSigning());

        Transaction tx1 = new TransactionImpl(txHeader, txSig.getSignature(), txBody);
        Transaction tx2 = new TransactionImpl(tx1.toBinary());

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        BlockBody blockBody1 = new BlockBody(txs1);

        BlockHeader blockHeader1 = new BlockHeader(
                chain, version, type, prevBlockHash, Long.MAX_VALUE, Long.MAX_VALUE,
                blockBody1.getMerkleRoot(), blockBody1.getStateRoot(), Long.MAX_VALUE);
        assertEquals(BlockHeader.LENGTH, blockHeader1.getBinaryForSigning().length);

        BlockHeader blockHeader2 = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.getStateRoot(), blockBody1.getLength());
        assertEquals(BlockHeader.LENGTH, blockHeader2.getBinaryForSigning().length);

        BlockHeader blockHeader3 = new BlockHeader(blockHeader1.toBinary());
        assertEquals(blockHeader1.toJsonObject(), blockHeader3.toJsonObject());
        assertArrayEquals(blockHeader1.getHashForSigning(), blockHeader3.getHashForSigning());

        BlockHeader blockHeader4 = new BlockHeader(blockHeader1.toBinary());
        assertEquals(blockHeader1.toJsonObject(), blockHeader4.toJsonObject());
        assertArrayEquals(blockHeader1.getHashForSigning(), blockHeader4.getHashForSigning());
    }

}
