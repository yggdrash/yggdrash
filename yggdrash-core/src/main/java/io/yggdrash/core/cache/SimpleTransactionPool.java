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

package io.yggdrash.core.cache;

import io.yggdrash.core.Transaction;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleTransactionPool implements TransactionPool {
    private final Cache<byte[], byte[]> cache;

    @Autowired
    public SimpleTransactionPool(Cache<byte[], byte[]> cache) {
        this.cache = cache;
    }

    @Override
    public Transaction getTxByHash(String id) {
        return null;
    }

    @Override
    public Transaction addTx(Transaction tx) {
        return null;
    }

    @Override
    public List getTxList() {
        return null;
    }

    @Override
    public void removeTx(List<String> hashList) {

    }

    public void put(byte[] key, byte[] value) {
        cache.put(key, value);
    }

    public void clear() {
        cache.clear();
    }

    public byte[] get(byte[] key) {
        return cache.get(key);
    }

    public Map<byte[], byte[]> getList(Set<byte[]> keys) {
        return cache.getAll(keys);
    }

    public void remove(Set<byte[]> keys) {
        cache.removeAll(keys);
    }
}
