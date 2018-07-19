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

import io.yggdrash.core.Transaction;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class SimpleTransactionPool implements TransactionPool {
    private static final Logger log = LoggerFactory.getLogger(SimpleTransactionPool.class);

    private final Cache<byte[], Transaction> cache;

    SimpleTransactionPool(Cache<byte[], Transaction> cache) {
        this.cache = cache;
    }

    @Override
    public Transaction get(byte[] key) {
        return cache.get(key);
    }

    @Override
    public Transaction put(byte[] key, Transaction tx) {
        cache.put(key, tx);
        return tx;
    }

    @Override
    public Map<byte[], Transaction> getAll(Set<byte[]> keys) {
        return cache.getAll(keys);
    }

    @Override
    public void remove(Set<byte[]> keys) {
        cache.removeAll(keys);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
