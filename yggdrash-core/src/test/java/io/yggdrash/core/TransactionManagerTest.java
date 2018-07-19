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

import io.yggdrash.TestUtils;
import io.yggdrash.core.store.StoreConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StoreConfiguration.class})
public class TransactionManagerTest {
    @Autowired
    TransactionManager tm;

    @Before
    public void setUp() throws Exception {
        tm.flush();
    }

    @Test
    public void shouldGetFromDb() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        byte[] key = dummyTx.getHash();
        tm.put(key, dummyTx);
        tm.batchAll();
        assertThat(tm.count()).isZero();
        Transaction foundValue = tm.get(key);
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldBatch() throws IOException {
        byte[] key = TestUtils.createDummyTx().getHash();
        tm.batchAll();
        assertThat(tm.count()).isZero();
    }

    @Test
    public void shouldGetFromPool() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        byte[] key = dummyTx.getHash().clone();
        tm.put(key, dummyTx);
        Transaction foundValue = tm.get(key);
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldPutByTxObject() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        tm.put(dummyTx.getHash(), dummyTx);
    }

    @Test
    public void shouldLoadTestObject() {
        assertThat(tm).isNotNull();
    }
}
