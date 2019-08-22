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

import io.yggdrash.common.config.Constants.ActiveProfiles;
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
import io.yggdrash.core.blockchain.osgi.Downloader;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.node.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableScheduling
public class BranchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final DefaultConfig defaultConfig;

    @Value("classpath:/branch-yggdrash.json")
    Resource yggdrashResource;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    SystemProperties systemProperties;

    @Autowired
    BranchConfiguration(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    // TODO Remove Default Branch Load
    @Profile(ActiveProfiles.NODE)
    @Bean
    BlockChain yggdrash(BranchGroup branchGroup) throws IOException {
        GenesisBlock genesis = GenesisBlock.of(yggdrashResource.getInputStream());
        BlockChain yggdrash = branchGroup.getBranch(genesis.getBranchId());
        if (yggdrash == null) {
            yggdrash = createBranch(genesis);
            branchGroup.addBranch(yggdrash);
        }
        return yggdrash;
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    BranchLoader branchLoader(DefaultConfig defaultConfig, BranchGroup branchGroup, Environment env) {

        BranchLoader branchLoader = new BranchLoader(defaultConfig.getBranchPath());
        boolean isValidator = Arrays.asList(env.getActiveProfiles()).contains(ActiveProfiles.VALIDATOR);
        boolean isBsNode = Arrays.asList(env.getActiveProfiles()).contains(ActiveProfiles.BOOTSTRAP);

        if (isValidator || isBsNode) {
            return branchLoader;
        }

        // TODO check exist branch
        try {
            for (GenesisBlock genesis : branchLoader.getGenesisBlockList()) {
                if (branchGroup.getBranch(genesis.getBranchId()) != null) {
                    continue;
                }
                BlockChain bc = createBranch(genesis);
                branchGroup.addBranch(bc);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return branchLoader;
    }

    private BlockChain createBranch(GenesisBlock genesis) {
        log.info("createBranch {} {}", genesis.getBranch().getBranchId(), genesis.getBranch().getName());

        try {
            Consensus consensus = new Consensus(genesis.getBranch().getConsensus());
            BranchId branchId = genesis.getBranch().getBranchId();
            BlockChainStoreBuilder builder = BlockChainStoreBuilder.newBuilder(branchId)
                    .withDataBasePath(defaultConfig.getDatabasePath())
                    .withProductionMode(defaultConfig.isProductionMode())
                    .setBlockStoreFactory(ValidatorService.blockStoreFactory())
                    .setConsensusAlgorithm(consensus.getAlgorithm())
            ;
            BlockChainStore blockChainStore = builder.build();
            BlockChain blockChain =
                    getBlockChain(defaultConfig, genesis, blockChainStore, branchId, systemProperties);

            log.info("Branch is Ready {}", blockChain.getBranchId());

            return blockChain;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    static BlockChain getBlockChain(DefaultConfig config,
                                    GenesisBlock genesis,
                                    BlockChainStore blockChainStore,
                                    BranchId branchId,
                                    SystemProperties systemProperties) {

        ContractStore contractStore = blockChainStore.getContractStore();

        BlockChainManager blockChainManager = new BlockChainManagerImpl(blockChainStore);

        FrameworkConfig frameworkConfig = new BootFrameworkConfig(config, branchId);
        FrameworkLauncher frameworkLauncher = new BootFrameworkLauncher(frameworkConfig);
        BundleServiceImpl bundleService = new BundleServiceImpl(frameworkLauncher.getBundleContext());

        ContractManager contractManager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(blockChainStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties) // Contract Executor. do not need contractManager.
                .build();

        return BlockChainBuilder.newBuilder()
                .setGenesis(genesis)
                .setBranchStore(blockChainStore.getBranchStore())
                .setBlockChainManager(blockChainManager)
                .setContractManager(contractManager)
                .setFactory(ValidatorService.factory())
                .build();
    }

    @Bean
    Downloader downloader(DefaultConfig defaultConfig) {
        return new Downloader(defaultConfig);
    }

}
