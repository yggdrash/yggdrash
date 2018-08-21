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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.datasource.DbSource;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TransactionStore implements Store<Sha3Hash, TransactionHusk> {
    private static final Logger log = LoggerFactory.getLogger(TransactionStore.class);

    private final DbSource<byte[], byte[]> db;
    private final Cache<Sha3Hash, TransactionHusk> huskTxPool;
    private final Set<Sha3Hash> unconfirmedTxs = new HashSet<>();


    public TransactionStore(DbSource db) {
        this.db = db;
        this.db.init();
        this.huskTxPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Sha3Hash.class, TransactionHusk.class,
                                ResourcePoolsBuilder.heap(10)));
    }

    /* method for test (start) */
    protected Wallet wallet;

    public void putDummyTx(String x) {
        try {
            wallet = new Wallet(new DefaultConfig());
        } catch (IOException | InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }

        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject param2 = new JsonObject();
        param2.addProperty("amount", x);
        params.add(param1);
        params.add(param2);

        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", "transfer");
        txObj.add("params", params);

        TransactionHusk tx = new TransactionHusk(txObj).sign(wallet);
        put(tx);
    }
    /* method for test (end) */

    @Override
    public Set<TransactionHusk> getAll() {
        try {
            List<byte[]> dataList = db.getAll();
            TreeSet<TransactionHusk> txSet = new TreeSet<>();
            for (byte[] data : dataList) {
                txSet.add(new TransactionHusk(data));
            }
            return txSet;
        } catch (IOException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return huskTxPool.containsKey(key) || db.get(key.getBytes()) != null;
    }

    @Override
    public void put(TransactionHusk tx) {
        huskTxPool.put(tx.getHash(), tx);
        unconfirmedTxs.add(tx.getHash());
    }

    @Override
    public TransactionHusk get(Sha3Hash key) throws InvalidProtocolBufferException {
        TransactionHusk item = huskTxPool.get(key);
        return item != null ? item : new TransactionHusk(db.get(key.getBytes()));
    }

    public void batchAll() {
        this.batch(unconfirmedTxs);
    }

    public void batch(Set<Sha3Hash> keys) {
        if (keys.size() > 0) {
            Map<Sha3Hash, TransactionHusk> map = huskTxPool.getAll(keys);
            for (Sha3Hash key : map.keySet()) {
                TransactionHusk foundTx = map.get(key);
                if (foundTx != null) {
                    db.put(key.getBytes(), foundTx.getData());
                }
            }
            this.flush();
        }
    }

    public Collection<TransactionHusk> getUnconfirmedTxs() {
        return huskTxPool.getAll(unconfirmedTxs).values();
    }

    public long countFromCache() {
        return unconfirmedTxs.size();
    }

    public long countFromDb() {
        return this.db.count();
    }

    public void flush() {
        huskTxPool.removeAll(unconfirmedTxs);
        unconfirmedTxs.clear();
    }
}
