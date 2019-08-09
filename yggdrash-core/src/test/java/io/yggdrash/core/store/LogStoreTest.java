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
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogStoreTest {
    private static final Logger log = LoggerFactory.getLogger(LogStoreTest.class);
    private LogStore store;
    private boolean prodMode;

    @Test
    public void prodTest() {
        isProdMode(false);
        putAndGetTest();
        isProdMode(true);
        putAndGetTest();
    }

    private void putAndGetTest() {
        long curIndex = store.size();

        if (!prodMode) {
            assertEquals(0, curIndex);
        }

        String txId = BlockChainTestUtils.createTransferTx().getHash().toString();
        String keyFormat = "%s/%d";

        String val0 = String.format(keyFormat, txId, 0);
        String val1 = String.format(keyFormat, txId, 1);
        String val2 = String.format(keyFormat, txId, 2);

        log.debug("\nput : {}\nput : {}\nput : {}", val0, val1, val2);

        store.put(val0);
        store.put(val1);
        store.put(val2);

        assertTrue(store.contains(curIndex));
        assertTrue(store.contains(curIndex + 1));
        assertTrue(store.contains(curIndex + 2));

        assertEquals(val0, store.get(curIndex));
        assertEquals(val1, store.get(curIndex + 1));
        assertEquals(val2, store.get(curIndex + 2));

        assertEquals(curIndex + 3, store.size());

        store.close();
    }

    private void isProdMode(boolean prod) {
        this.prodMode = prod;
        if (prod) {
            store = new LogStore(new LevelDbDataSource(new DefaultConfig().getDatabasePath(), "log"));
        } else {
            store = new LogStore(new HashMapDbSource());
        }
    }
}