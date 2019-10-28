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

import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReceiptStoreTest {

    private ReceiptStore store;

    @Before
    public void setUp() {
        store = new ReceiptStore(new HashMapDbSource());
    }

    @Test
    public void testPutReceipt() {
        Receipt receipt = new ReceiptImpl();
        receipt.setTxId("TEST_TRANSACTION");
        receipt.setStatus(ExecuteStatus.SUCCESS);

        store.put(receipt);
        Assert.assertNotNull(store.get("TEST_TRANSACTION"));
    }

    @Test
    public void testReceipt() {
        Receipt receipt = new ReceiptImpl();
        receipt.setTxId("TEST_TRANSACTION_1234512345");
        receipt.setStatus(ExecuteStatus.SUCCESS);
        store.put(receipt);

        Receipt receipt2 = store.get("TEST_TRANSACTION_1234512345");

        Assert.assertTrue(receipt.getTxId().equalsIgnoreCase(receipt2.getTxId()));
        Assert.assertEquals(receipt.getStatus(), receipt2.getStatus());
    }

}
