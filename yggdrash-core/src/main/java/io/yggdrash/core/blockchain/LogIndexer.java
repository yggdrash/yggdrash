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

import io.yggdrash.core.store.LogStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class LogIndexer {
    private static final Logger log = LoggerFactory.getLogger(LogIndexer.class);
    private static final String keyFormat = "%s/%d";
    private static final String keySeparator = "/";

    private final LogStore logStore; //<logIndex : txId + indexOfReceipt>
    private final TransactionReceiptStore receiptStore; //<txHash : txReceipt>

    public LogIndexer(LogStore logStore, TransactionReceiptStore receiptStore) {
        this.logStore = logStore;
        this.receiptStore = receiptStore;
    }

    public void put(String txId, int size) { //TODO check log duplicated
        //log.debug("put logs : txId = {}, size = {}", txId, size);
        IntStream.range(0, size).mapToObj(i -> String.format(keyFormat, txId, i)).forEach(logStore::put);
    }

    public String get(long logIndex) { //TODO check log contained
        return logStore.get(logIndex);
    }

    public String getLog(long logIndex) {
        String val = get(logIndex);
        int separator = val.indexOf(keySeparator);
        String txId = val.substring(0, separator);
        int indexOfReceipt = Integer.parseInt(val.substring(separator + 1));

        return receiptStore.get(txId).getTxLog().get(indexOfReceipt);
    }

    public List<String> getLogs(long start, long offset) {
        long end = offset > curIndex() ? curIndex() : offset;
        return LongStream.range(start, end).mapToObj(this::getLog).collect(Collectors.toList());
    }

    public long curIndex() {
        return logStore.curIndex();
    }

    public boolean contains(long logIndex) {
        return logStore.contains(logIndex);
    }

    public void close() {
        log.debug("close logStore");
        logStore.close();
    }
}