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

public class LogStore {
    private final DbSource<byte[], byte[]> db;
    private long index;

    public LogStore(DbSource<byte[], byte[]> db) { //<logIndex : txId + indexOfReceipt>
        this.index = 0;
        this.db = db.init();
    }

    public void put(String value) {
        db.put(Longs.toByteArray(index++), value.getBytes());
    }

    public String get(long index) {
        return new String(db.get(Longs.toByteArray(index)));
    }

    public long curIndex() {
        return index;
    }

    public boolean contains(long index) {
        return get(index) != null;
    }

    public void close() {
        this.db.close();
    }
}