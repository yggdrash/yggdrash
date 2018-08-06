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

package io.yggdrash.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.core.store.CachePool;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.proto.BlockChainProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionStore {
    private static final Logger log = LoggerFactory.getLogger(TransactionStore.class);

    private final DbSource db;
    private final CachePool<String, Transaction> txPool;
    private final Set<String> unconfirmedTxSet = new HashSet<>();

    public TransactionStore(DbSource db, CachePool transactionPool) {
        this.db = db;
        this.db.init();
        this.txPool = transactionPool;
    }

    public Transaction put(Transaction tx) {
        try {
            txPool.put(tx);
            unconfirmedTxSet.add(tx.getHashString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tx;
    }

    public Transaction get(String key) {
        Transaction foundTx = txPool.get(key);
        return foundTx != null ? foundTx : deserialize(db.get(key.getBytes()));
    }

    public void batchAll() {
        this.batch(unconfirmedTxSet);
    }

    public void batch(Set<String> keys) {
        if (keys.size() > 0) {
            Map<String, Transaction> map = txPool.getAll(keys);
            for (String key : map.keySet()) {
                Transaction foundTx = map.get(key);
                if (foundTx != null) {
                    db.put(key.getBytes(), serialize(foundTx));
                }
            }
            this.flush();
        }
    }

    public Collection<Transaction> getUnconfirmedTxs() {
        Map<String, Transaction> unconfirmedTxs = txPool.getAll(unconfirmedTxSet);
        return unconfirmedTxs.values();
    }

    public int count() {
        return unconfirmedTxSet.size();
    }

    public void flush() {
        txPool.remove(unconfirmedTxSet);
        unconfirmedTxSet.clear();
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
