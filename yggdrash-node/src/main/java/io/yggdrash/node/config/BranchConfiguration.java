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
import io.yggdrash.core.contract.ContractClassLoader;
import io.yggdrash.core.contract.ContractLoader;
import io.yggdrash.core.contract.ContractManager;
import io.yggdrash.core.contract.ContractMeta;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Value("classpath:/branch-stem.json")
    Resource stemResource;

    @Value("classpath:/branch-yeed.json")
    Resource yeedResource;

    @Value("classpath:/branch-sw.json")
    Resource swResource;

    @Value("classpath:/branch-asset.json")
    Resource assetResource;

    BranchConfiguration(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
    }

    @Bean
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain stem(PeerGroup peerGroup, BranchGroup branchGroup)
            throws IOException {
        return addBranch(stemResource.getInputStream(), peerGroup, branchGroup);
    }

    @Bean
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain yeed(PeerGroup peerGroup, BranchGroup branchGroup)
            throws IOException {
        return addBranch(yeedResource.getInputStream(), peerGroup, branchGroup);
    }

    @Bean
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain sw(PeerGroup peerGroup, BranchGroup branchGroup)
            throws IOException {
        return addBranch(swResource.getInputStream(), peerGroup, branchGroup);
    }

    @Bean
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain asset(PeerGroup peerGroup, BranchGroup branchGroup)
            throws IOException {
        return addBranch(assetResource.getInputStream(), peerGroup, branchGroup);
    }

    @Bean
    BranchGroup branchGroup(BranchLoader loader, PeerGroup peerGroup) {
        BranchGroup branchGroup = new BranchGroup();
        try {
            for (GenesisBlock genesis : loader.getGenesisBlockList()) {
                addBranch(genesis, peerGroup, branchGroup);
            }
        } catch (Exception e2) {
            log.warn(e2.getMessage(), e2);
        }
        return branchGroup;
    }

    @Bean
    BranchLoader branchLoader(DefaultConfig defaultConfig) {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        return new BranchLoader(defaultConfig.getBranchPath());
    }

    @Bean
    ContractLoader contractLoader(DefaultConfig defaultConfig) {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        return new ContractLoader(defaultConfig.getContractPath());
    }

    private BlockChain addBranch(InputStream is, PeerGroup peerGroup, BranchGroup branchGroup)
            throws IOException {
        GenesisBlock genesis = GenesisBlock.of(is);
        return addBranch(genesis, peerGroup, branchGroup);
    }

    private BlockChain addBranch(GenesisBlock genesis, PeerGroup peerGroup,
                                 BranchGroup branchGroup) {
        try {
            BlockChain branch = BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .build();
            branchGroup.addBranch(branch, peerGroup);
            PeerStore peerStore = storeBuilder.buildPeerStore(branch.getBranchId());
            peerGroup.addPeerTable(branch.getBranchId(), peerStore);
            return branch;
        } catch (Exception e) {
            log.warn(e.getMessage());
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
