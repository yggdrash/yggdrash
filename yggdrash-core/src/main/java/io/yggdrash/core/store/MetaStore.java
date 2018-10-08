/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;

public class MetaStore implements Store<MetaStore.MetaInfo, Sha3Hash> {
    private final DbSource<byte[], byte[]> db;

    public MetaStore() {
        db = new LevelDbDataSource("meta").init();
    }

    @Override
    public void put(MetaInfo key, Sha3Hash value) {
        db.put(key.name().getBytes(), value.getBytes());
    }

    @Override
    public Sha3Hash get(MetaInfo key) {
        return new Sha3Hash(db.get(key.name().getBytes()));
    }

    @Override
    public boolean contains(MetaInfo key) {
        return false;
    }

    @Override
    public void close() {
        db.close();
    }

    public enum MetaInfo {
        RECENT_BLOCK
    }
}
