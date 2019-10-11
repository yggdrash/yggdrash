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
import io.yggdrash.core.store.ReceiptStore;
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
    private final ReceiptStore receiptStore; //<txHash : txReceipt>

    public LogIndexer(LogStore logStore, ReceiptStore receiptStore) {
        this.logStore = logStore;
        this.receiptStore = receiptStore;
    }

    public void put(String txId, int size) { //TODO check log duplicated
        log.trace("put logs : txId = {}, size = {}", txId, size);
        IntStream.range(0, size).mapToObj(i -> String.format(keyFormat, txId, i)).forEach(logStore::put);
    }

    public String get(long logIndex) { //TODO check log contained
        return logStore.get(logIndex);
    }

    public Log getLog(long logIndex) {
        if (logIndex < 0 || logIndex > curIndex()) {
            return Log.createBy(logIndex, "", "Log not exists");
        }

        String val = get(logIndex);
        int separator = val.indexOf(keySeparator);
        String txId = val.substring(0, separator);
        int indexOfReceipt = Integer.parseInt(val.substring(separator + 1));
        String log = receiptStore.get(txId).getLog().get(indexOfReceipt);

        return Log.createBy(logIndex, txId, log);
    }

    public List<Log> getLogs(long from, long offset) {
        long start = from < 0 ? 0 : from;
        long end = start + offset > curIndex() ? curIndex() : start + offset;
        return LongStream.rangeClosed(start, end).mapToObj(this::getLog).collect(Collectors.toList());
    }

    public long curIndex() {
        return logStore.size() != 0 ? logStore.size() - 1 : 0;
    }

    public boolean contains(long logIndex) {
        return logStore.contains(logIndex);
    }

    public void close() {
        log.debug("close logStore");
        logStore.close();
    }
}