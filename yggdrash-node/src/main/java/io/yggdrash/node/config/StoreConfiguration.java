/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.config;

import io.yggdrash.contract.StateStore;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.File;

@Configuration
public class StoreConfiguration {

    private final NodeProperties nodeProperties;

    @Autowired
    StoreConfiguration(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Bean
    BlockChain blockChain(BlockStore blockStore) {
        return new BlockChain(new File(getClass().getClassLoader().getResource("branch-sample.json").getFile()));
    }

    @Bean
    BlockStore blockStore(@Qualifier("blockDbSource") DbSource source) {
        return new BlockStore(source);
    }

    @Bean
    TransactionStore transactionStore(@Qualifier("txDbSource") DbSource source) {
        return new TransactionStore(source);
    }

    @Profile("prod")
    @Primary
    @Bean(name = "blockDbSource")
    DbSource blockLevelDbSource() {
        return new LevelDbDataSource(nodeProperties.getChainId() + "/blocks");
    }

    @Profile("prod")
    @Primary
    @Bean(name = "txDbSource")
    DbSource txLevelDbSource() {
        return new LevelDbDataSource(nodeProperties.getChainId() + "/txs");
    }

    @Bean(name = "blockDbSource")
    DbSource blockDbSource() {
        return new HashMapDbSource();
    }

    @Bean(name = "txDbSource")
    DbSource txDbSource() {
        return new HashMapDbSource();
    }

    @Bean
    StateStore stateStore() {
        return new StateStore();
    }
}
