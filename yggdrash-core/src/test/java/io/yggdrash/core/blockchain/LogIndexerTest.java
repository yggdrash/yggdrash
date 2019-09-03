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

package io.yggdrash.core.blockchain;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.core.store.LogStore;
import io.yggdrash.core.store.ReceiptStore;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogIndexerTest {

    private static final String logFormat = "[%d] Transfer 100 from 0xa to 0xb";
    private static final String keyFormat = "%s/%d";
    private LogStore logStore;
    private ReceiptStore receiptStore;
    private LogIndexer logIndexer;
    private List<Sha3Hash> txHashes;

    @Before
    public void setUp() throws Exception {
        logStore = new LogStore(new HashMapDbSource());
        receiptStore = new ReceiptStore(new HashMapDbSource());
        logIndexer = new LogIndexer(logStore, receiptStore);
        txHashes = new ArrayList<>();

        init();
    }

    @Test
    public void getValueTest() {
        for (int i = 0; i < 10; i++) {
            assertEquals(String.format(keyFormat, txHashes.get(i), 9), logIndexer.get(10 * i + 9));
            assertEquals(String.format(keyFormat, txHashes.get(i), 0), logIndexer.get(10 * i));
        }
    }

    @Test
    public void getLogTest() {
        for (int i = 0; i < 100; i++) {
            assertEquals(String.format(logFormat, i % 10), logIndexer.getLog(i).getMsg());
        }
    }

    @Test
    public void getLogsTest() {
        int i = 0;
        for (Log log : logIndexer.getLogs(0, 9)) {
            assertEquals(String.format(logFormat, i++), log.getMsg());
        }
    }

    private void init() {
        int size = 10;

        for (Transaction tx : generateTxs(size)) {
            txHashes.add(tx.getHash());
            receiptStore.put(generateReceipt(tx, size));
            logIndexer.put(tx.getHash().toString(), size);

            assertTrue(receiptStore.contains(tx.getHash().toString()));
            assertEquals(size, receiptStore.get(tx.getHash().toString()).getLog().size());
        }
        assertEquals(100, logStore.size());
        assertEquals(100, logStore.getIndex());
    }

    private List<Transaction> generateTxs(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> BlockChainTestUtils.createTransferTx())
                .collect(Collectors.toList());
    }

    private Receipt generateReceipt(Transaction tx, int size) {
        Receipt receipt = new ReceiptImpl(
                tx.getHash().toString(), tx.getLength(), tx.getAddress().toString());

        IntStream.range(0, size)
                .mapToObj(i -> String.format(logFormat, i))
                .collect(Collectors.toList())
                .forEach(receipt::addLog);

        return receipt;
    }
}