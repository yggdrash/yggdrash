/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionReceiptStoreTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionReceiptStoreTest.class);

    TransactionReceiptStore store;



    @Before
    public void setUp() {
        store = new TransactionReceiptStore(new HashMapDbSource());
    }

    @Test
    public void testPutTransactionReceipt() {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setTxId("TEST_TRANSACTION");
        receipt.setStatus(ExecuteStatus.SUCCESS);

        store.put(receipt);
    }

    @Test
    public void testTransactionReceipt() {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setTxId("TEST_TRANSACTION_1234512345");
        receipt.setStatus(ExecuteStatus.SUCCESS);
        store.put(receipt);

        TransactionReceipt receipt2 = store.get("TEST_TRANSACTION_1234512345");

        assert receipt.getTxId().equalsIgnoreCase(receipt2.getTxId());
        assert receipt.getStatus() == receipt2.getStatus();

    }

}
