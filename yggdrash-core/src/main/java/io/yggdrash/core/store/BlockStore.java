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
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.datasource.DbSource;

public class BlockStore implements Store<Sha3Hash, BlockHusk> {
    private final DbSource<byte[], byte[]> db;

    BlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(Sha3Hash key, BlockHusk value) {
        db.put(key.getBytes(), value.getData());
    }

    @Override
    public BlockHusk get(Sha3Hash key) {
        byte[] foundValue = db.get(key.getBytes());
        if (foundValue != null) {
            return new BlockHusk(foundValue);
        }

        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return db.get(key.getBytes()) != null;
    }

    public void close() {
        this.db.close();
    }
}
