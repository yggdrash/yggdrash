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

import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.core.store.CachePool;
import io.yggdrash.core.store.HashMapTransactionPool;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionStoreTest {

    TransactionStore ts;

    @Before
    public void setUp() {
        DbSource db = new HashMapDbSource();
        CachePool pool = new HashMapTransactionPool();
        ts = new TransactionStore(db, pool);
        ts.flush();
    }

    @Test
    public void shouldGetFromDb() throws InvalidProtocolBufferException {
        String body = "1";
        TransactionHusk tx = new TransactionHusk(body);
        Sha3Hash key = tx.getHash();
        ts.put(key, tx);

        ts.batchAll();
        TransactionHusk transactionHusk = ts.get(key);
        assertThat(transactionHusk).isEqualTo(tx);
    }

    @Test
    public void shouldBeBatched() {
        String body = "1";
        TransactionHusk tx = new TransactionHusk(body);
        Sha3Hash key = tx.getHash();
        ts.put(key, tx);

        ts.batchAll();
        assertThat(ts.countFromCache()).isZero();
        assertThat(ts.countFromDb()).isEqualTo(1L);
    }

    @Test
    public void shouldBeGotTxFromCache() throws InvalidProtocolBufferException {
        String body = "1";
        TransactionHusk tx = new TransactionHusk(body);
        Sha3Hash key = tx.getHash();
        ts.put(key, tx);

        TransactionHusk foundTx = ts.get(key);
        assertThat(foundTx).isNotNull();
        assertThat(foundTx.getBody()).isEqualTo(body);
    }

    @Test
    public void shouldBePutTx() {
        TransactionHusk tx = new TransactionHusk("1");
        ts.put(tx.getHash(), tx);
    }

    @Test
    public void shouldLoadTestObject() {
        assertThat(ts).isNotNull();
    }
}
