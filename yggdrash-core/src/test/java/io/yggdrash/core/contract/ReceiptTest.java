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

package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.core.blockchain.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReceiptTest {

    private Receipt txReceipt;

    @Before
    public void setUp() {
        txReceipt = new ReceiptImpl();
    }

    @Test
    public void logTest() {
        JsonObject testLog = new JsonObject();
        testLog.addProperty("key", "value");
        txReceipt.addLog(testLog.toString());
        Assert.assertEquals("{\"key\":\"value\"}", txReceipt.getLog().get(0));
    }

    @Test
    public void statusTest() {
        Assert.assertFalse(txReceipt.isSuccess());
        txReceipt.setStatus(ExecuteStatus.SUCCESS);
        Assert.assertTrue(txReceipt.isSuccess());
    }

    @Test
    public void transactionHashTest() {
        Transaction tx = BlockChainTestUtils.createTransferTx();
        String txId = tx.getHash().toString();
        txReceipt.setTxId(txId);
        Assert.assertEquals(txId, txReceipt.getTxId());
    }

}