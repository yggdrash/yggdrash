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

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.store.HashMapTransactionPool;
import io.yggdrash.core.store.TransactionPool;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionManagerTest {

    TransactionManager tm;

    @Before
    public void setUp() {
        DbSource db = new HashMapDbSource();
        TransactionPool pool = new HashMapTransactionPool();
        tm = new TransactionManager(db, pool);
        tm.flush();
    }

    @Test
    public void shouldGetFromDb() {
        Transaction dummyTx = TestUtils.createDummyTx();
        tm.put(dummyTx);
        tm.batchAll();
        assertThat(tm.count()).isZero();
        Transaction foundValue = tm.get(dummyTx.getHashString());
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldBatch() {
        byte[] key = TestUtils.createDummyTx().getHash();
        tm.batchAll();
        assertThat(tm.count()).isZero();
    }

    @Test
    public void shouldGetFromPool() {
        Transaction dummyTx = TestUtils.createDummyTx();
        byte[] key = dummyTx.getHash().clone();
        tm.put(dummyTx);
        Transaction foundValue = tm.get(dummyTx.getHashString());
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldPutByTxObject() throws IOException, InvalidCipherTextException {
        Transaction tx = new Transaction(new Wallet(), new JsonObject());
        tm.put(tx);
    }

    @Test
    public void shouldLoadTestObject() {
        assertThat(tm).isNotNull();
    }
}
