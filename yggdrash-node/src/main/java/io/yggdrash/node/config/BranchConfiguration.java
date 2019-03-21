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
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.PrepareBlockchain;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import java.io.File;
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

    @Value("${es.host:#{null}}")
    private String esHost;
    @Value("${es.transport:#{null}}")
    private String esTransport;
    @Value("${event.store:#{null}}")
    private String[] eventStore;

    @Autowired
    BranchConfiguration(StoreBuilder storeBuilder) {
        log.info("Branch Path : {}", storeBuilder.getConfig().getBranchPath());
        this.storeBuilder = storeBuilder;
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

        SystemProperties systemProperties = SystemProperties.SystemPropertiesBuilder.aSystemProperties()
                .withEsHost(esHost)
                .withEsTransport(esTransport)
                .withEventStore(eventStore)
                .build();
        try {
            BlockChain bc = BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .setPolicyLoader(policyLoader)
                    .setSystemProperties(systemProperties)
                    .build();

            // Check BlockChain is Ready
            PrepareBlockchain prepareBlockchain = new PrepareBlockchain(storeBuilder.getConfig());

            // check block chain is ready
            if (prepareBlockchain.checkBlockChainIsReady(bc)) {
                // TODO install bundles
                // bc.getContractContainer()
                ContractContainer container = bc.getContractContainer();
                for(BranchContract contract : bc.getBranchContracts()) {
                    File branchContractFile = prepareBlockchain.loadContractFile(contract.getContractVersion());
                    container.installContract(contract.getContractVersion(), branchContractFile, contract.isSystem());
                }
                container.reloadInject();
            } else {
                // TODO blockchain ready fails
                log.error("Blockchain is not Ready");
                return null;
            }

            log.info("Branch is Ready %s", bc.getBranch().getBranchId());

            return bc;
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
