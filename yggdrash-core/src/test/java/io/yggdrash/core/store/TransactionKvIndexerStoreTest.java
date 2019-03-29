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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionKvIndexerStoreTest {

    private TransactionKvIndexerStore txKvIndexerStore;

    @Before
    public void setUp() {
        txKvIndexerStore = new TransactionKvIndexerStore(new HashMapDbSource());
        assertThat(txKvIndexerStore).isNotNull();
    }

    @Test
    public void overallDatabaseTest() { // flow :  put -> contains -> get -> search -> intersection
        // verification before put data
        assertEquals(0, txKvIndexerStore.getTags().size());

        // create the data and put it in the store(db)
        byte[] txHash1 = BlockChainTestUtils.createTransferTxHusk().getHashByte();
        byte[] txHash2 = BlockChainTestUtils.createBranchTxHusk().getHashByte();
        assertFalse(Arrays.equals(txHash1, txHash2));

        String tag1 = "method";
        String value1 = "transfer";

        txKvIndexerStore.put(tag1, value1, txHash1); // "method/transfer/0"
        txKvIndexerStore.put(tag1, value1, txHash1); // "method/transfer/1"

        String tag2 = "account";
        String value2 = BlockChainTestUtils.createTransferTxHusk().getAddress().toString();

        txKvIndexerStore.put(tag2, value2, txHash1); // "account/ee1a821e3e5996a4ff3facd0978a4313c10af1cf/0"
        txKvIndexerStore.put(tag2, value2, txHash2); // "account/ee1a821e3e5996a4ff3facd0978a4313c10af1cf/1"

        // verification after put data
        assertTrue(txKvIndexerStore.containsTag(tag1));
        assertTrue(txKvIndexerStore.containsTag(tag2));
        assertTrue(txKvIndexerStore.containsValue(tag1, value1));
        assertTrue(txKvIndexerStore.containsValue(tag2, value2));
        assertFalse(txKvIndexerStore.containsValue(tag1, value2));
        assertFalse(txKvIndexerStore.containsValue(tag2, value1));
        assertEquals(2, txKvIndexerStore.getTags().size());

        // get data from the store and compare with txHash
        byte[] key1 = String.format("%s/%s/%d", tag1, value1, 0).getBytes();
        assertArrayEquals(txHash1, txKvIndexerStore.get(key1));
        byte[] key2 = String.format("%s/%s/%d", tag1, value1, 1).getBytes();
        assertArrayEquals(txHash1, txKvIndexerStore.get(key2));
        byte[] key3 = String.format("%s/%s/%d", tag2, value2, 0).getBytes();
        assertArrayEquals(txHash1, txKvIndexerStore.get(key3));
        byte[] key4 = String.format("%s/%s/%d", tag2, value2, 1).getBytes();
        assertArrayEquals(txHash2, txKvIndexerStore.get(key4));

        // search data from the store
        assertEquals(1, txKvIndexerStore.search(tag1, value1).size());
        assertEquals(2, txKvIndexerStore.search(tag2, value2).size());
        assertEquals(0, txKvIndexerStore.search(tag1, value2).size());
        assertEquals(0, txKvIndexerStore.search(tag2, value1).size());

        // intersect data
        assertEquals(1, txKvIndexerStore.intersection(tag1, value1, tag2, value2).size());
        assertTrue(txKvIndexerStore.intersection(tag1, value1, tag2, value2).contains(txHash1));
    }
}