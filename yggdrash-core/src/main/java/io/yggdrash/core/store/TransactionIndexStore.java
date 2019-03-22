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

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.exception.NonExistObjectException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransactionIndexStore implements ReadWriterStore<byte[], byte[]> {
    private final DbSource<byte[], byte[]> db;

    public TransactionIndexStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
    }

    @Override
    public void put(byte[] key, byte[] value) {
        db.put(key, value);
    }

    @Override
    public boolean contains(byte[] key) {
        return db.get(key) != null;
    }

    @Override
    public void close() {
        this.db.close();
    }

    @Override
    public byte[] get(byte[] key) {
        byte[] foundValue = db.get(key);
        if (foundValue != null) {
            return foundValue;
        }
        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    public byte[] get(String key) {
        return get(key.getBytes());
    }

    public List<byte[]> getByTag(byte[] tag) {
        List<byte[]> res = new ArrayList<>();
        try {
            res = db.getKeySetByValue(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    public int size() {
        int res = 0;
        try {
            res = db.getAll().size();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }
}
