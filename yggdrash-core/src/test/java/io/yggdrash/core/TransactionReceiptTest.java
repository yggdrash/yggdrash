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

package io.yggdrash.core;

import io.yggdrash.TestUtils;
import org.junit.Before;
import org.junit.Test;

public class TransactionReceiptTest {

    private TransactionReceipt txReceipt;

    @Before
    public void setUp() {
        txReceipt = new TransactionReceipt();
    }

    @Test
    public void logTest() {
        txReceipt.putLog("key", "value");
        assert String.valueOf(txReceipt.getLog("key")).equals("value");
    }

    @Test
    public void statusTest() {
        assert !txReceipt.isSuccess();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);
        assert txReceipt.isSuccess();
    }

    @Test
    public void transactionHashTest() {
        TransactionHusk tx = TestUtils.createTransferTxHusk();
        String hashOfTx = tx.getHash().toString();
        txReceipt.setTransactionHash(hashOfTx);
        assert txReceipt.getTransactionHash().equals(hashOfTx);
    }

    @Test
    public void getBlockHashTest() {
        assert txReceipt.getBlockHash().equals(
                "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b");
    }

    @Test
    public void getYeedUsedTest() {
        assert txReceipt.getYeedUsed() == 30000;
        assert txReceipt.toString().contains("yeedUsed=30000");
    }
}