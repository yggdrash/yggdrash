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
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTransactionPoolTest {
    public static final Logger log = LoggerFactory.getLogger(SimpleTransactionPoolTest.class);

    private TransactionPool txPool;

    @Before
    public void setUp() {

        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        Cache<String, Transaction> cache = cacheManager.createCache("txCache",
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, Transaction.class,
                                ResourcePoolsBuilder.heap(10)));
        txPool = new SimpleTransactionPool(cache);
    }

    @Test
    public void shouldClearPool() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        txPool.put(dummyTx);
        txPool.clear();
        Transaction foundValue = txPool.get(dummyTx.getHashString());
        assertThat(foundValue).isNull();
    }

    @Test
    public void shouldGetObject() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        txPool.put(dummyTx);

        Transaction foundValue = txPool.get(dummyTx.getHashString());
        assertThat(foundValue).isEqualTo(dummyTx);
    }

    @Test
    public void shouldPutTx() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        txPool.put(dummyTx);
    }

    @Test
    public void shouldBeLoad() {
        assertThat(txPool).isNotNull();
    }
}
