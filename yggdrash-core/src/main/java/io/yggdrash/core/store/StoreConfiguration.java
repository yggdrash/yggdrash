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

import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionManager;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StoreConfiguration.class);

    @Bean
    DbSource levelDbDataSource() {
        return new LevelDbDataSource("tx");
    }

    @Bean
    TransactionPool transactionPool() {
        return new SimpleTransactionPool(txCache());
    }

    @Bean
    Cache txCache() {
        log.debug("=== Create cache for transaction");
        return cacheManager().createCache("txCache",
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, Transaction.class,
                                ResourcePoolsBuilder.heap(10)));
    }

    @Bean
    CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder().build(true);
    }

    @Bean
    TransactionManager transactionManager() {
        return new TransactionManager(levelDbDataSource(), transactionPool());
    }
}
