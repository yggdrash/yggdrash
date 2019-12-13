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
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.exception.NonExistObjectException;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionStore implements ReadWriterStore<Sha3Hash, Transaction> {
    private static final Logger log = LoggerFactory.getLogger(TransactionStore.class);
    private static final Lock lock = new ReentrantLock();
    private static final int CACHE_SIZE = 100000;

    private final DbSource<byte[], byte[]> db;

    // Shared resources(pendingPool, pendingKeys, stateRoot) must be synchronized.
    private final Cache<Sha3Hash, Transaction> pendingPool;
    private final Queue<Sha3Hash> pendingKeys;

    private Queue<Transaction> readCache;

    public TransactionStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
        this.pendingPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Sha3Hash.class, Transaction.class,
                                ResourcePoolsBuilder.heap(CACHE_SIZE)));
        this.pendingKeys = EvictingQueue.create(CACHE_SIZE);
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
        if (key == null || key.getBytes() == null) {
            log.warn("contains() is failed. key is not valid.");
            return false;
        }

        //TODO should be tested
        //lock.lock();
        try {
            return pendingPool.containsKey(key) || db.get(key.getBytes()) != null;
        } catch (Exception e) {
            log.warn("contains() is failed. {} {}", e.getMessage(), key.toString());
            return false;
        }

        //finally {
        //    lock.unlock();
        //}
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void put(Sha3Hash key, Transaction tx) {
        lock.lock();
        try {
            if (!contains(key)) {
                pendingPool.put(key, tx);
                if (pendingPool.containsKey(key)) {
                    pendingKeys.add(key);
                } else {
                    log.debug("unconfirmedTxs size={}, ignore key={}", pendingKeys.size(), key);
                }
            }
        } catch (Exception e) {
            log.warn("put() is failed. {} {}", e.getMessage(), key.toString());
        } finally {
            lock.unlock();
        }
    }

    public void addTransaction(Transaction tx) {
        put(tx.getHash(), tx);
    }

    @Override
    public Transaction get(Sha3Hash key) {
        Transaction item = pendingPool.get(key);
        try {
            return item != null ? item : new TransactionImpl(db.get(key.getBytes()));
        } catch (Exception e) {
            log.warn("get() is failed. {} {}", e.getMessage(), key.toString());
            throw new NonExistObjectException.TxNotFound(key.toString());
        }
    }

    public void batch(Set<Sha3Hash> keys) {
        if (keys.isEmpty()) {
            return;
        }

        lock.lock();
        try {
            Map<Sha3Hash, Transaction> map = pendingPool.getAll(keys);
            for (Map.Entry<Sha3Hash, Transaction> entry : map.entrySet()) {
                Transaction foundTx = entry.getValue();
                if (foundTx != null) {
                    db.put(entry.getKey().getBytes(), foundTx.toBinary());
                    addReadCache(foundTx);
                }
            }
            this.flush(keys);
        } finally {
            lock.unlock();
        }
    }

    private void addReadCache(Transaction tx) {
        readCache.add(tx);
    }


    public List<Transaction> getUnconfirmedTxsWithLimit(long limit) {
        lock.lock();
        try {
            long bodySizeSum = 0;
            List<Transaction> unconfirmedTxs = new ArrayList<>(pendingKeys.size());
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
            return unconfirmedTxs;
        } finally {
            lock.unlock();
        }
    }

    public Collection<Transaction> getUnconfirmedTxs() {
        lock.lock();
        try {
            Collection<Transaction> unconfirmedTxs = getTransactionList();
            if (!unconfirmedTxs.isEmpty()) {
                log.trace("unconfirmedKeys={} unconfirmedTxs={}", pendingKeys.size(), unconfirmedTxs.size());
            }
            return unconfirmedTxs;
        } finally {
            lock.unlock();
        }
    }

    public int getUnconfirmedTxsSize() {
        return this.pendingKeys.size();
    }

    private List<Transaction> getTransactionList() {
        List<Transaction> unconfirmedTxs = new ArrayList<>();
        for (Sha3Hash key : pendingKeys) {
            unconfirmedTxs.add(pendingPool.get(key));
        }
        return unconfirmedTxs;
    }

    public void flush(Set<Sha3Hash> keys) {
        pendingPool.removeAll(keys);
        pendingKeys.removeAll(keys);
        log.trace("flushSize={} remainPendingSize={}", keys.size(), pendingKeys.size());
    }

    public void updateCache(Block block) {
        lock.lock();
        try {
            List<Transaction> body = block.getBody().getTransactionList();
            this.readCache.addAll(body);
        } finally {
            lock.unlock();
        }
    }
}
