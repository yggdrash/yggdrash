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
import io.yggdrash.core.husk.TransactionHusk;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.util.Map;
import java.util.Set;

public class HuskTransactionPool implements CachePool<Sha3Hash, TransactionHusk> {
    private Cache<Sha3Hash, TransactionHusk> cache = CacheManagerBuilder
            .newCacheManagerBuilder().build(true)
            .createCache("txCache", CacheConfigurationBuilder
                    .newCacheConfigurationBuilder(Sha3Hash.class, TransactionHusk.class,
                            ResourcePoolsBuilder.heap(10)));

    @Override
    public TransactionHusk get(Sha3Hash key) {
        return cache.get(key);
    }

    @Override
    public TransactionHusk put(TransactionHusk tx) {
        cache.put(tx.getHash(), tx);
        return tx;
    }

    @Override
    public Map<Sha3Hash, TransactionHusk> getAll(Set<Sha3Hash> keys) {
        return cache.getAll(keys);
    }

    @Override
    public void remove(Set<Sha3Hash> keys) {
        cache.removeAll(keys);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
