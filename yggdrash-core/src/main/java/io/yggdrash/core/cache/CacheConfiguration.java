package io.yggdrash.core.cache;

import io.yggdrash.core.SimpleTransactionPool;
import io.yggdrash.core.datasource.LevelDbDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    // TODO 캐쉬 설정을 아래 부분에 적용 합니다.


    @Bean
    LevelDbDataSource levelDbDataSource() {
        return new LevelDbDataSource("tx");
    }

    @Bean
    SimpleTransactionPool transactionPool() {
        return new SimpleTransactionPool();
    }
}
