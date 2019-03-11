/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.config;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableScheduling
public class BranchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final StoreBuilder storeBuilder;

    @Value("classpath:/branch-yggdrash.json")
    Resource yggdrashResource;

    @Autowired
    BranchConfiguration(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
    }

    @Bean
    @ConditionalOnProperty(name = "yggdrash.node.chain.enabled", matchIfMissing = true)
    BlockChain yggdrash(BranchGroup branchGroup, ContractPolicyLoader contractPolicyLoader) throws IOException {
        BlockChain yggdrash = createBranch(yggdrashResource.getInputStream(), contractPolicyLoader);
        branchGroup.addBranch(yggdrash);
        return yggdrash;
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    ContractPolicyLoader contractPolicyLoader() {
        return new ContractPolicyLoader();
    }

    @Bean
    BranchLoader branchLoader(DefaultConfig defaultConfig, BranchGroup branchGroup, ContractPolicyLoader policyLoader) {
        BranchLoader branchLoader = new BranchLoader(defaultConfig.getBranchPath());
        try {
            for (GenesisBlock genesis : branchLoader.getGenesisBlockList()) {
                BlockChain bc = createBranch(genesis, policyLoader);
                branchGroup.addBranch(bc);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return branchLoader;
    }

    private BlockChain createBranch(InputStream is, ContractPolicyLoader policyLoader)
            throws IOException {
        GenesisBlock genesis = GenesisBlock.of(is);
        return createBranch(genesis, policyLoader);
    }

    private BlockChain createBranch(GenesisBlock genesis, ContractPolicyLoader policyLoader) {
        try {
            return BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .setPolicyLoader(policyLoader)
                    .build();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Scheduling Beans
     */
    @Bean
    @ConditionalOnProperty("yggdrash.node.chain.gen")
    public ChainTask chainTask() {
        return new ChainTask();
    }
}
