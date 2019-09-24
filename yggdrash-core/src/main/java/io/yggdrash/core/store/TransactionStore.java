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

import com.google.common.collect.EvictingQueue;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionStore implements ReadWriterStore<Sha3Hash, Transaction> {
    private static final Logger log = LoggerFactory.getLogger(TransactionStore.class);
    private static final Lock lock = new ReentrantLock();

    private static final int CACHE_SIZE = 500;
    private long countOfTxs = 0;

    private Queue<Transaction> readCache;
    private Sha3Hash stateRoot;
    private final Cache<Sha3Hash, Transaction> pendingPool;
    private final Set<Sha3Hash> pendingKeys = new HashSet<>();
    private final DbSource<byte[], byte[]> db;

    public TransactionStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
        this.pendingPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Sha3Hash.class, Transaction.class,
                                ResourcePoolsBuilder.heap(Long.MAX_VALUE)));
        this.readCache = EvictingQueue.create(CACHE_SIZE);
    }

    TransactionStore(DbSource<byte[], byte[]> db, int cacheSize) {
        this(db);
        this.readCache = EvictingQueue.create(cacheSize);
    }

    public Collection<Transaction> getRecentTxs() {
        return new ArrayList<>(readCache);
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return pendingPool.containsKey(key) || db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void put(Sha3Hash key, Transaction tx) {
        pendingPool.put(key, tx);
        if (pendingPool.containsKey(key)) {
            pendingKeys.add(key);
        } else {
            log.debug("unconfirmedTxs size={}, ignore key={}", pendingKeys.size(), key);
        }
    }

    public void addTransaction(Transaction tx, Sha3Hash stateRoot) {
        lock.lock();
        try {
            if (stateRoot != null) {
                setStateRoot(stateRoot);
            }
            put(tx.getHash(), tx);
        } finally {
            lock.unlock();
        }
    }

    public void setStateRoot(Sha3Hash stateRoot) {
        this.stateRoot = stateRoot;
    }

    @Override
    public Transaction get(Sha3Hash key) {
        Transaction item = pendingPool.get(key);
        try {
            return item != null ? item : new TransactionImpl(db.get(key.getBytes()));
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    public void batch(Set<Sha3Hash> keys) {
        if (keys.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            Map<Sha3Hash, Transaction> map = pendingPool.getAll(keys);
            int countOfBatchedTxs = map.size();
            for (Map.Entry<Sha3Hash, Transaction> entry : map.entrySet()) {
                Transaction foundTx = entry.getValue();
                if (foundTx != null) {
                    db.put(entry.getKey().getBytes(), foundTx.toBinary());
                    addReadCache(foundTx);
                } else {
                    countOfBatchedTxs -= 1;
                }
            }
            this.countOfTxs += countOfBatchedTxs;
            this.flush(keys);
        } finally {
            lock.unlock();
        }
    }

    private void addReadCache(Transaction tx) {
        readCache.add(tx);
    }

    public long countOfTxs() {
        return this.countOfTxs;
    }

    public List<Transaction> getUnconfirmedTxsWithLimit(long limit) {
        List<Transaction> unconfirmedTxs;
        lock.lock();
        try {
            long bodySizeSum = 0;
            unconfirmedTxs = new ArrayList<>(pendingKeys.size());
            for (Sha3Hash key : pendingKeys) {
                Transaction tx = pendingPool.get(key);
                if (tx != null) {
                    bodySizeSum += tx.getLength();
                    if (bodySizeSum > limit) {
                        break;
                    }
                    unconfirmedTxs.add(tx);
                }
            }
        } finally {
            lock.unlock();
        }
        return unconfirmedTxs;
    }

    public Collection<Transaction> getUnconfirmedTxs() {
        Collection<Transaction> unconfirmedTxs;
        lock.lock();
        try {
            unconfirmedTxs = pendingPool.getAll(pendingKeys).values();
            if (!unconfirmedTxs.isEmpty()) {
                log.debug("unconfirmedKeys={} unconfirmedTxs={}", pendingKeys.size(), unconfirmedTxs.size());
            }
        } finally {
            lock.unlock();
        }
        return unconfirmedTxs;
    }

    public Map<Sha3Hash, List<Transaction>> getUnconfirmedTxsWithStateRoot() {
        Map<Sha3Hash, List<Transaction>> result;
        lock.lock();
        try {
            Sha3Hash stateRootHash = stateRoot;
            List<Transaction> unconfirmedTxs = new ArrayList<>(pendingPool.getAll(pendingKeys).values());
            if (!unconfirmedTxs.isEmpty()) {
                log.debug("unconfirmedKeys={} unconfirmedTxs={}", pendingKeys.size(), unconfirmedTxs.size());
            }
            result = new HashMap<>();
            result.put(stateRootHash, unconfirmedTxs);
        } finally {
            lock.unlock();
        }
        return result;
    }

    public void flush(Set<Sha3Hash> keys) {
        pendingPool.removeAll(keys);
        pendingKeys.removeAll(keys);
        log.trace("flushSize={} remainPendingSize={}", keys.size(), pendingKeys.size());
    }

    public void updateCache(List<Transaction> body) {
        this.countOfTxs += body.size();
        this.readCache.addAll(body);
    }
}
