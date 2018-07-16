package io.yggdrash.core.cache;

import io.yggdrash.core.TransactionManager;
import io.yggdrash.core.datasource.LevelDbDataSource;
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
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    LevelDbDataSource levelDbDataSource() {
        return new LevelDbDataSource("tx");
    }

    @Bean
    SimpleTransactionPool simpleTransactionPool() {
        return new SimpleTransactionPool(txCache());
    }

    @Bean
    Cache txCache() {
        log.debug("=== Create cache for transaction");
        return cacheManager().createCache("txCache",
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(byte[].class, byte[].class,
                                ResourcePoolsBuilder.heap(10)));
    }

    @Bean
    CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder().build(true);
    }

    @Bean
    TransactionManager transactionManager() {
        return new TransactionManager(levelDbDataSource(), simpleTransactionPool());
    }
}
