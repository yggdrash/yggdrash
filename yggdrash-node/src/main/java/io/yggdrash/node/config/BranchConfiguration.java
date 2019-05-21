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
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainManagerImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.blockchain.osgi.ContractManagerBuilder;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import io.yggdrash.node.service.ValidatorService;
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

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    SystemProperties systemProperties;

    @Autowired
    BranchConfiguration(DefaultConfig defaultConfig) {
        this.storeBuilder = StoreBuilder.newBuilder().setConfig(defaultConfig);
    }

    // TODO Remove Default Branch Load
    @Bean
    @ConditionalOnProperty(name = "yggdrash.node.chain.enabled", matchIfMissing = true)
    BlockChain yggdrash(BranchGroup branchGroup, ContractPolicyLoader policyLoader) throws IOException {
        BlockChain yggdrash = createBranch(yggdrashResource.getInputStream(), policyLoader);
        branchGroup.addBranch(yggdrash);
        return yggdrash;
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    ContractPolicyLoader policyLoader() {
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
            BranchId branchId = genesis.getBranch().getBranchId();
            storeBuilder.setBranchId(branchId)
                    .setConsensusAlgorithm(consensus.getAlgorithm())
                    .setBlockStoreFactory(ValidatorService.blockStoreFactory());

            BlockChain blockChain = getBlockChain(genesis, storeBuilder, policyLoader, branchId, systemProperties);

            log.info("Branch is Ready {}", blockChain.getBranchId());

            return blockChain;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    static BlockChain getBlockChain(GenesisBlock genesis, StoreBuilder storeBuilder,
                                    ContractPolicyLoader policyLoader, BranchId branchId,
                                    SystemProperties systemProperties) {

        ContractStore contractStore = storeBuilder.buildContractStore();

        BlockChainManager blockChainManager = new BlockChainManagerImpl(
                storeBuilder.buildBlockStore(),
                storeBuilder.buildTransactionStore(),
                contractStore.getTransactionReceiptStore());

        ContractManager contractManager = ContractManagerBuilder.newInstance()
                .withFrameworkFactory(policyLoader.getFrameworkFactory())
                .withContractManagerConfig(policyLoader.getContractManagerConfig())
                .withBranchId(branchId.toString())
                .withContractStore(contractStore)
                .withConfig(storeBuilder.getConfig())
                .withSystemProperties(systemProperties)
                .build();

        return BlockChainBuilder.newBuilder()
                .setGenesis(genesis)
                .setBranchStore(contractStore.getBranchStore())
                .setBlockChainManager(blockChainManager)
                .setContractManager(contractManager)
                .setFactory(ValidatorService.factory())
                .build();
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
