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

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.contract.ContractClassLoader;
import io.yggdrash.core.contract.ContractManager;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

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
    BlockChain yggdrash(BranchGroup branchGroup) throws IOException {
        // TODO remove yggdrash
        InputStream is = yggdrashResource.getInputStream();
        GenesisBlock genesis = GenesisBlock.of(is);

        BlockChain yggdrash = loadBranch(genesis.getBranch().getBranchId());

        if (yggdrash == null) {
            yggdrash = createBranch(genesis);
        }

        branchGroup.addBranch(yggdrash);
        return yggdrash;
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    BranchLoader branchLoader(DefaultConfig defaultConfig, BranchGroup branchGroup) {
        BranchLoader branchLoader = new BranchLoader(defaultConfig.getBranchPath());
        // TODO check exist branch
        try {
            for (GenesisBlock genesis : branchLoader.getGenesisBlockList()) {
                // check exist branch
                BlockChain bc = loadBranch(genesis.getBranch().getBranchId());
                if (bc == null) {
                    bc = createBranch(genesis);
                }
                // TODO check Validator
                branchGroup.addBranch(bc);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return branchLoader;
    }

    @Bean
    ContractManager contractManager(DefaultConfig defaultConfig) {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        return new ContractManager(defaultConfig.getContractPath());
    }

    private BlockChain createBranch(InputStream is)
            throws IOException {
        GenesisBlock genesis = GenesisBlock.of(is);
        return createBranch(genesis);
    }

    private BlockChain createBranch(GenesisBlock genesis) {
        // TODO createBranch Save Branch File
        try {
            return BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .build();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    // TODO load Branch By Store
    private BlockChain loadBranch(BranchId branchId) {
        // TODO blockChain Loader will load blockchain by store
        // Load Meta Store
        BlockChainBuilder builder = BlockChainBuilder.Builder();
        MetaStore metaStore = storeBuilder.buildMetaStore(branchId);
        if (!branchId.equals(metaStore.getBranchId())) {
            return null;
        }
        BlockStore blockStore = storeBuilder.buildBlockStore(branchId);

        // TODO get Branch
        Branch branch = metaStore.getBranch();

        // TODO get Genesis
        Sha3Hash genesisBlockHash = metaStore.getGenesisBlockHash();
        BlockHusk genesisBlock = blockStore.get(genesisBlockHash);
        GenesisBlock genesis = GenesisBlock.of(branch, genesisBlock);

        // TODO Get Contracts


        return builder.addGenesis(genesis)
            .setBlockStore(blockStore)
            .setMetaStore(metaStore)
            .setStoreBuilder(storeBuilder)
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
