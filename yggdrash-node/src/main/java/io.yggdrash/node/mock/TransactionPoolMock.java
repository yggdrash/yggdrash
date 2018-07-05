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
import io.yggdrash.core.format.TransactionFormat;
import io.yggdrash.core.TransactionPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TransactionPoolMock implements TransactionPool {
    private Map<String, TransactionFormat> txs = new HashMap<>();

    @Override
    public TransactionFormat getTxByHash(String id) {
        return txs.get(id);
    }

    @Override
    public TransactionFormat addTx(TransactionFormat tx) throws IOException {
        System.out.println("TransactionPoolMock :: addTx : " + tx);
        txs.put(tx.getHashString(), tx);
        return tx;
    }
}
