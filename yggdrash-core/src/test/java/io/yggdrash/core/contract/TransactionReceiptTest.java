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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.junit.Before;
import org.junit.Test;

public class TransactionReceiptTest {

    private TransactionReceipt txReceipt;

    @Before
    public void setUp() {
        txReceipt = new TransactionReceiptImpl();
    }

    @Test
    public void logTest() {
        txReceipt.putLog("key", "value");
        assert String.valueOf(txReceipt.getLog("key")).equals("value");
    }

    @Test
    public void statusTest() {
        assert !txReceipt.isSuccess();
        txReceipt.setStatus(ExecuteStatus.SUCCESS);
        assert txReceipt.isSuccess();
    }

    @Test
    public void transactionHashTest() {
        TransactionHusk tx = BlockChainTestUtils.createTransferTxHusk();
        String txId = tx.getHash().toString();
        txReceipt.setTxId(txId);
        assert txReceipt.getTxId().equals(txId);
    }

}