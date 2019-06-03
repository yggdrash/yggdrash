/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.store;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogStoreTest {
    private LogStore store;

    @Before
    public void setUp() throws Exception {
        store = new LogStore(new HashMapDbSource());
    }

    @Test
    public void putAndGetTest() {
        String txId = BlockChainTestUtils.createTransferTx().getHash().toString();
        String keyFormat = "%s/%d";

        String val0 = String.format(keyFormat, txId, 0);
        String val1 = String.format(keyFormat, txId, 0);
        String val2 = String.format(keyFormat, txId, 0);

        store.put(val0);
        store.put(val1);
        store.put(val2);

        assertTrue(store.contains(0));
        assertTrue(store.contains(1));
        assertTrue(store.contains(2));

        assertEquals(val0, store.get(0));
        assertEquals(val1, store.get(1));
        assertEquals(val2, store.get(2));

        assertEquals(3, store.curIndex());
    }
}