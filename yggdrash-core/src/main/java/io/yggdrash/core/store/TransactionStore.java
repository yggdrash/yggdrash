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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.proto.BlockChainProto;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionStore implements Store<Sha3Hash, TransactionHusk> {
    private final DbSource<byte[], byte[]> db;
    private final CachePool<String, Transaction> txPool;
    private final Cache<Sha3Hash, TransactionHusk> huskTxPool;
    private final Set<String> unconfirmedTxSet = new HashSet<>();
    private final Set<Sha3Hash> unconfirmedTxs = new HashSet<>();


    public TransactionStore(DbSource db, CachePool transactionPool) {
        this.db = db;
        this.db.init();
        this.txPool = transactionPool;
        this.huskTxPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Sha3Hash.class, TransactionHusk.class,
                                ResourcePoolsBuilder.heap(10)));
    }

    @Deprecated
    public Transaction put(Transaction tx) {
        try {
            txPool.put(tx);
            unconfirmedTxSet.add(tx.getHashString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tx;
    }

    @Override
    public void put(Sha3Hash key, TransactionHusk tx) {
        huskTxPool.put(key, tx);
        unconfirmedTxs.add(key);
    }

    @Deprecated
    public Transaction get(String key) {
        Transaction foundTx = txPool.get(key);
        return foundTx != null ? foundTx : deserialize(db.get(key.getBytes()));
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

    public Collection<Transaction> getUnconfirmedTxs() {
        Map<String, Transaction> unconfirmedTxs = txPool.getAll(unconfirmedTxSet);
        return unconfirmedTxs.values();
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

    private byte[] serialize(Transaction transaction) {
        return convertToProto(transaction).toByteArray();
    }

    private Transaction deserialize(byte[] stream) {
        return convertToObject(stream);
    }

    private BlockChainProto.Transaction convertToProto(Transaction tx) {
        TransactionHeader txHeader = tx.getHeader();
        BlockChainProto.TransactionHeader header =
                BlockChainProto.TransactionHeader.newBuilder()
                        .setType(ByteString.copyFrom(txHeader.getType()))
                        .setVersion(ByteString.copyFrom(txHeader.getVersion()))
                        .setDataHash(ByteString.copyFrom(txHeader.getDataHash()))
                        .setDataSize(txHeader.getDataSize())
                        .setTimestamp(txHeader.getTimestamp())
                        .setSignature(ByteString.copyFrom(txHeader.getSignature()))
                        .build();
        return BlockChainProto.Transaction.newBuilder()
                .setHeader(header)
                .setData(tx.getData())
                .build();
    }

    private Transaction convertToObject(byte[] stream) {
        BlockChainProto.Transaction txProto = null;
        try {
            txProto = BlockChainProto.Transaction.parseFrom(stream);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        if (txProto == null) {
            return null;
        }

        BlockChainProto.TransactionHeader txHeaderProto = txProto.getHeader();
        TransactionHeader txHeader = new TransactionHeader(
                txHeaderProto.getType().toByteArray(),
                txHeaderProto.getVersion().toByteArray(),
                txHeaderProto.getDataHash().toByteArray(),
                txHeaderProto.getDataSize(),
                txHeaderProto.getTimestamp(),
                txHeaderProto.getSignature().toByteArray()
        );

        return new Transaction(txHeader, txProto.getData());
    }
}
