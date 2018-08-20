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

package io.yggdrash.core.store.datasource;

import io.yggdrash.TestUtils;
import io.yggdrash.core.TransactionHusk;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class EhCacheSourceTest {
    private static final Logger log = LoggerFactory.getLogger(EhCacheSourceTest.class);

    Cache<String, TransactionHusk> cache;

    @Before
    public void setUp() {

        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        this.cache = cacheManager.createCache("txCache",
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, TransactionHusk.class,
                                ResourcePoolsBuilder.heap(10)));
    }

    @Test
    public void shouldGetTx() {
        TransactionHusk dummyTx = TestUtils.createTxHusk();
        cache.put(dummyTx.getHash().toString(), dummyTx);
        TransactionHusk foundTx = cache.get(dummyTx.getHash().toString());
        assertThat(foundTx).isEqualTo(dummyTx);
    }

    @Test
    public void shouldPutTx() {
        TransactionHusk dummyTx = TestUtils.createTxHusk();
        cache.put(dummyTx.getHash().toString(), dummyTx);
    }

    @Test
    public void shouldBeLoadedBean() {
        assertThat(cache).isNotNull();
    }
}
