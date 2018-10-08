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
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.datasource.DbSource;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionStore implements Store<Sha3Hash, TransactionHusk> {
    private static final Logger log = LoggerFactory.getLogger(TransactionStore.class);
    private static final Lock LOCK = new ReentrantLock();

    private final DbSource<byte[], byte[]> db;
    private final Cache<Sha3Hash, TransactionHusk> huskTxPool;
    private final Set<Sha3Hash> unconfirmedTxs = new HashSet<>();

    public TransactionStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
        this.huskTxPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Sha3Hash.class, TransactionHusk.class,
                                ResourcePoolsBuilder.heap(Long.MAX_VALUE)));
    }

    public Set<TransactionHusk> getAll() {
        try {
            List<byte[]> dataList = db.getAll();
            TreeSet<TransactionHusk> txSet = new TreeSet<>();
            for (byte[] data : dataList) {
                txSet.add(new TransactionHusk(data));
            }
            return txSet;
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return huskTxPool.containsKey(key) || db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void put(Sha3Hash key, TransactionHusk tx) {
        LOCK.lock();
        huskTxPool.put(key, tx);
        if (huskTxPool.containsKey(key)) {
            unconfirmedTxs.add(key);
        } else {
            log.warn("unconfirmedTxs size={}, ignore key={}", unconfirmedTxs.size(), key);
        }
        LOCK.unlock();
    }

    @Override
    public TransactionHusk get(Sha3Hash key) {
        TransactionHusk item = huskTxPool.get(key);
        try {
            return item != null ? item : new TransactionHusk(db.get(key.getBytes()));
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    public void batch(Set<Sha3Hash> keys) {
        LOCK.lock();
        if (keys.size() > 0) {
            Map<Sha3Hash, TransactionHusk> map = huskTxPool.getAll(keys);
            for (Sha3Hash key : map.keySet()) {
                TransactionHusk foundTx = map.get(key);
                if (foundTx != null) {
                    db.put(key.getBytes(), foundTx.getData());
                }
            }
            this.flush(keys);
        }
        LOCK.unlock();
    }

    public Collection<TransactionHusk> getUnconfirmedTxs() {
        return huskTxPool.getAll(unconfirmedTxs).values();
    }

    private void flush(Set<Sha3Hash> keys) {
        huskTxPool.removeAll(keys);
        unconfirmedTxs.removeAll(keys);
    }
}
