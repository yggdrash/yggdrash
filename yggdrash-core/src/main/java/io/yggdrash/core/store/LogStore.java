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

import com.google.common.primitives.Longs;
import io.yggdrash.common.store.datasource.DbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class LogStore {
    private static final Logger log = LoggerFactory.getLogger(LogStore.class);
    private final DbSource<byte[], byte[]> db;
    private long index;

    public LogStore(DbSource<byte[], byte[]> db) { //<logIndex : txId + indexOfReceipt>
        this.db = db.init();
        this.index = getIndex();
        log.debug("Current Log Index : {}", index);
    }

    public long getIndex() {
        byte[] originIndex = db.get("index".getBytes());
        return originIndex != null ? ByteBuffer.wrap(originIndex).getLong() : 0;
    }

    private void putIndex() {
        db.put("index".getBytes(), Longs.toByteArray(index));
    }

    public void put(String value) {
        db.put(Longs.toByteArray(index++), value.getBytes());
        putIndex();
    }

    public String get(long index) {
        return new String(db.get(Longs.toByteArray(index)));
    }

    public long size() {
        return index;
    }

    public boolean contains(long index) {
        return get(index) != null;
    }

    public void close() {
        putIndex();
        log.debug("Close LogStore. Current Log Index : {}", index);
        this.db.close();
    }
}