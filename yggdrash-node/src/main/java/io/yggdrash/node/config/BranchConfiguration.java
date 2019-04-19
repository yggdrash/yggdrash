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
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.gateway.controller.BlockChainCollector;
import io.yggdrash.gateway.store.es.EsClient;
import io.yggdrash.node.ChainTask;
import io.yggdrash.node.service.ValidatorService;
import org.elasticsearch.common.util.set.Sets;
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

    @Value("${es.host:#{null}}")
    private String esHost;
    @Value("${es.transport:#{null}}")
    private String esTransport;
    @Value("${event.store:#{null}}")
    private String[] eventStore;

    @Autowired
    BranchConfiguration(DefaultConfig defaultConfig) {
        this.storeBuilder = StoreBuilder.newBuilder().setConfig(defaultConfig);
    }

    // TODO Remove Default Branch Load
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
        // TODO check exist branch
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
        log.info("createBranch {} {}", genesis.getBranch().getBranchId(), genesis.getBranch().getName());

        try {
            Consensus consensus = new Consensus(genesis.getBranch().getConsensus());
            storeBuilder.setConsensusAlgorithm(consensus.getAlgorithm())
                    .setBlockStoreFactory(ValidatorService.blockStoreFactory());

            BlockChain bc = BlockChainBuilder.newBuilder()
                    .setGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .setPolicyLoader(policyLoader)
                    .setFactory(ValidatorService.factory())
                    .build();

            log.info("Branch is Ready {}", bc.getBranchId());

            return bc;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    @Bean
    @ConditionalOnProperty("es.host")
    public BlockChainCollector EsClient() {
        return new BlockChainCollector(
                EsClient.newInstance(esHost, Integer.parseInt(esTransport), Sets.newHashSet()));
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
