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

package io.yggdrash.node.mock;

import io.yggdrash.core.Transaction;
import io.yggdrash.core.store.TransactionPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionPoolMock implements TransactionPool {
    private final Map<byte[], Transaction> txs = new ConcurrentHashMap<>();

    @Override
    public Transaction get(byte[] key) {
        return txs.get(key);
    }

    @Override
    public Transaction put(byte[] key, Transaction tx) throws IOException {
        txs.put(tx.getHash(), tx);
        return tx;
    }

    @Override
    public Map<byte[], Transaction> getAll(Set<byte[]> keys) {
        Map<byte[], Transaction> result = new HashMap<>();
        for (byte[] key : keys) {
            result.put(key, txs.get(key));
        }
        return result;
    }

    @Override
    public void remove(Set<byte[]> keys) {
        for (byte[] key : keys) {
            txs.remove(key);
        }
    }

    @Override
    public void clear() {
        txs.clear();
    }
}
