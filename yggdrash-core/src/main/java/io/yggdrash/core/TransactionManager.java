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

import io.yggdrash.core.store.SimpleTransactionPool;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class TransactionManager {
    private final LevelDbDataSource db;
    private final SimpleTransactionPool txPool;
    private final Set<byte[]> unconfirmedTxSet = new HashSet<>();

    @Autowired
    public TransactionManager(LevelDbDataSource db, SimpleTransactionPool simpleTransactionPool) {
        this.db = db;
        this.db.init();
        this.txPool = simpleTransactionPool;
    }

    public void put(Transaction tx) throws IOException {
        this.put(tx.getHash(), tx.getData().getBytes());
    }

    public void put(byte[] key, byte[] value) {
        txPool.put(key, value);
        unconfirmedTxSet.add(key);
    }

    public byte[] get(byte[] key) {
        byte[] foundTx = txPool.get(key);
        return foundTx != null ? foundTx : db.get(key);
    }

    public void batch() {
        Map<byte[], byte[]> map = txPool.getList(unconfirmedTxSet);
        for (byte[] key : map.keySet()) {
            db.put(key, map.get(key));
        }
        this.flush();
    }

    public int count() {
        return unconfirmedTxSet.size();
    }

    private void flush() {
        txPool.remove(unconfirmedTxSet);
        unconfirmedTxSet.clear();
    }
}
