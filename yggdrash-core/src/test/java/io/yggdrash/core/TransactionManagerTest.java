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
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.store.StoreConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.crypto.InvalidCipherTextException;
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

    @Test
    public void shouldGetFromDb() {
        byte[] key = putDummyTx();
        tm.batch();
        assertThat(tm.count()).isZero();
        byte[] foundValue = tm.get(key);
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldBatch() {
        putDummyTx();
        tm.batch();
        assertThat(tm.count()).isZero();
    }

    private byte[] putDummyTx() {
        byte[] key = TestUtils.randomBytes(32);
        byte[] value = TestUtils.randomBytes(32);
        tm.put(key, value);
        return key;
    }

    @Test
    public void shouldGetFromPool() {
        byte[] key = putDummyTx();
        byte[] foundValue = tm.get(key);
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldPutByBytes() {
        byte[] key = TestUtils.randomBytes(32);
        byte[] value = TestUtils.randomBytes(32);
        tm.put(key, value);
        assertThat(tm.count()).isEqualTo(1);
    }

    @Test
    public void shouldPutByTxObject() throws IOException, InvalidCipherTextException {
        tm.put(new Transaction(new Wallet(new DefaultConfig()), new JsonObject()));
    }

    @Test
    public void shouldLoadTestObject() {
        assertThat(tm).isNotNull();
    }
}
