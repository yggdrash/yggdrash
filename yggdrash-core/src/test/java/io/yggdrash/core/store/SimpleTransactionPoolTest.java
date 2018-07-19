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

package io.yggdrash.core.store;

import io.yggdrash.TestUtils;
import io.yggdrash.core.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StoreConfiguration.class)
public class SimpleTransactionPoolTest {
    public static final Logger log = LoggerFactory.getLogger(SimpleTransactionPoolTest.class);

    @Autowired
    TransactionPool txPool;

    @Test
    public void shouldClearPool() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        txPool.put(dummyTx.getHash(), dummyTx);
        txPool.clear();
        Transaction foundValue = txPool.get(dummyTx.getHash());
        assertThat(foundValue).isNull();
    }

    @Test
    public void shouldGetObject() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        byte[] key = dummyTx.getHash();
        txPool.put(key, dummyTx);

        Transaction foundValue = txPool.get(key);
        assertThat(foundValue).isEqualTo(dummyTx);
    }

    @Test
    public void shouldPutTx() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        txPool.put(dummyTx.getHash(), dummyTx);
    }

    @Test
    public void shouldBeLoad() {
        assertThat(txPool).isNotNull();
    }
}
