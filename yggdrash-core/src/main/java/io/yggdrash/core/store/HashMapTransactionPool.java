/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.core.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapTransactionPool implements TransactionPool {
    private final Map<String, Transaction> txs = new ConcurrentHashMap<>();

    @Override
    public Transaction get(String key) {
        return txs.get(key);
    }

    @Override
    public Transaction put(Transaction tx) {
        txs.put(tx.getHashString(), tx);
        return tx;
    }

    @Override
    public Map<String, Transaction> getAll(Set<String> keys) {
        Map<String, Transaction> result = new HashMap<>();
        for (String key : keys) {
            Transaction foundTx = txs.get(key);
            if (foundTx != null) {
                result.put(key, foundTx);
            }
        }
        return result;
    }

    @Override
    public void remove(Set<String> keys) {
        for (String key : keys) {
            txs.remove(key);
        }
    }

    @Override
    public void clear() {
        txs.clear();
    }
}
