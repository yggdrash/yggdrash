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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionPoolMock implements TransactionPool {
    private final Map<String, Transaction> txs = new ConcurrentHashMap<>();

    @Override
    public Transaction getTxByHash(String id) {
        return txs.get(id);
    }

    @Override
    public Transaction addTx(Transaction tx) throws IOException {
        if (txs.containsKey(tx.getHashString())) {
            return null;
        }
        txs.put(tx.getHashString(), tx);
        return tx;
    }

    @Override
    public List<Transaction> getTxList() {
        return new ArrayList(txs.values());
    }

    @Override
    public void removeTx(List<String> hashList) {
        for (String id : hashList) {
            txs.remove(id);
        }
    }

}
