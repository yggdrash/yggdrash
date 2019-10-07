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
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Block;
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

    private final DbSource<byte[], byte[]> db;

    // Shared resources(pendingPool, pendingKesy, stateRoot) must be synchoronized.
    private final Cache<Sha3Hash, Transaction> pendingPool;
    private final List<Sha3Hash> pendingKeys = new ArrayList<>();
    private Sha3Hash stateRoot = new Sha3Hash(Constants.EMPTY_HASH, true);

    private Queue<Transaction> readCache;

    private long countOfTxs = 0;

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
        if (key == null || key.getBytes() == null) {
            log.warn("contains() is failed. key is not valid.");
            return false;
        }

        lock.lock();
        try {
            return pendingPool.containsKey(key) || db.get(key.getBytes()) != null;
        } catch (Exception e) {
            log.warn("contains() is failed. {} {}", e.getMessage(), key.toString());
            return false;
        } finally {
            lock.unlock();
        }
    }

    private boolean containsUnlock(Sha3Hash key) {
        if (key == null || key.getBytes() == null) {
            log.warn("contains() is failed. key is not valid.");
            return false;
        }

        try {
            return pendingPool.containsKey(key) || db.get(key.getBytes()) != null;
        } catch (Exception e) {
            log.warn("contains() is failed. {} {}", e.getMessage(), key.toString());
            return false;
        }
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void put(Sha3Hash key, Transaction tx) {
        lock.lock();
        try {
            log.trace("put() before() txId={} stateRoot={}", tx.getHash().toString(), this.stateRoot.toString());

            if (!containsUnlock(key)) {
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
            log.trace("put() after() txId={} stateRoot={}", tx.getHash().toString(), this.stateRoot.toString());
            lock.unlock();
        }
    }

    private void putUnlock(Sha3Hash key, Transaction tx) {
        log.debug("putUnlock() before() txId={} stateRoot={}", tx.getHash().toString(), this.stateRoot.toString());
        if (!containsUnlock(key)) {
            pendingPool.put(key, tx);
            if (pendingPool.containsKey(key)) {
                pendingKeys.add(key);
            } else {
                log.debug("unconfirmedTxs size={}, ignore key={}", pendingKeys.size(), key);
            }
        }
        log.debug("putUnlock() after() txId={} stateRoot={}", tx.getHash().toString(), this.stateRoot.toString());
    }

    public void addTransaction(Transaction tx) {
        put(tx.getHash(), tx);
    }

    public void addTransaction(Transaction tx, Sha3Hash stateRoot) {
        lock.lock();
        try {
            log.debug("addTransaction() tx={} stateRoot={}", tx.getHash().toString(), stateRoot);
            if (!containsUnlock(tx.getHash())) {
                putUnlock(tx.getHash(), tx);
                this.stateRoot = new Sha3Hash(stateRoot.getBytes(), true);
                log.debug("addTransaction() is success tx={} stateRoot={}", tx.getHash().toString(), stateRoot);
                return;
            }
        } catch (Exception e) {
            log.warn("addTransaction() is failed. tx={} stateRoot={} {}",
                    tx.getHash().toString(), stateRoot, e.getMessage());
        } finally {
            lock.unlock();
        }
        log.debug("addTransaction() is failed tx={} stateRoot={}", tx.getHash().toString(), stateRoot);
    }

    @Override
    public Transaction get(Sha3Hash key) {
        Transaction item = pendingPool.get(key);
        try {
            return item != null ? item : new TransactionImpl(db.get(key.getBytes()));
        } catch (Exception e) {
            log.warn("get() is failed. {} {}", e.getMessage(), key.toString());
            throw new FailedOperationException(e);
        }
    }

    public void batch(Set<Sha3Hash> keys, Sha3Hash stateRoot) {
        if (keys == null || keys.isEmpty() || stateRoot == null || stateRoot.getBytes() == null) {
            log.debug("batch() is failed. keys or stateRoot is not valid.");
            return;
        }

        lock.lock();
        this.stateRoot = new Sha3Hash(stateRoot.getBytes(), true);
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
            this.flushUnlock(keys);
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
        lock.lock();
        try {
            long bodySizeSum = 0;
            List<Transaction> unconfirmedTxs = new ArrayList<>(pendingKeys.size());
            for (Sha3Hash key : pendingKeys) {
                Transaction tx = new TransactionImpl(pendingPool.get(key));
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
                log.debug("unconfirmedKeys={} unconfirmedTxs={}", pendingKeys.size(), unconfirmedTxs.size());
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
            unconfirmedTxs.add(new TransactionImpl(pendingPool.get(key)));
        }
        return unconfirmedTxs;
    }

    public Map<Sha3Hash, List<Transaction>> getUnconfirmedTxsWithStateRoot() {
        lock.lock();
        try {
            List<Transaction> unconfirmedTxs = getTransactionList();
            if (!unconfirmedTxs.isEmpty()) {
                log.debug("unconfirmedKeys={} unconfirmedTxs={}", pendingKeys.size(), unconfirmedTxs.size());
            }
            Map<Sha3Hash, List<Transaction>> result = new HashMap<>();
            result.put(new Sha3Hash(this.stateRoot.getBytes(), true), unconfirmedTxs);
            return result;
        } finally {
            lock.unlock();
        }
    }

    public void flush(Set<Sha3Hash> keys) {
        lock.lock();
        try {
            pendingPool.removeAll(keys);
            pendingKeys.removeAll(keys);
            log.trace("flushSize={} remainPendingSize={}", keys.size(), pendingKeys.size());
        } finally {
            lock.unlock();
        }
    }

    public void flush(Sha3Hash key) {
        lock.lock();
        try {
            pendingPool.remove(key);
            pendingKeys.remove(key);
            log.trace("remainPendingSize={}", pendingKeys.size());
        } finally {
            lock.unlock();
        }
    }

    private void flushUnlock(Set<Sha3Hash> keys) {
        pendingPool.removeAll(keys);
        pendingKeys.removeAll(keys);
        log.trace("flushSize={} remainPendingSize={}", keys.size(), pendingKeys.size());
    }

    public void updateCache(Block block) {
        lock.lock();
        try {
            List<Transaction> body = block.getBody().getTransactionList();
            this.stateRoot = new Sha3Hash(block.getHeader().getStateRoot(), true);
            this.countOfTxs += body.size();
            this.readCache.addAll(body);
        } finally {
            lock.unlock();
        }
    }
}
