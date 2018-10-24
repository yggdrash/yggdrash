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

import io.yggdrash.contract.ContractEvent;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockChainBuilder;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.event.ContractEventListener;
import io.yggdrash.core.net.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class BranchConfiguration implements ContractEventListener {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final boolean isProduction;
    private final PeerGroup peerGroup;

    private BranchGroup branchGroup;

    @Value("classpath:/genesis.json")
    private Resource resource;

    void setResource(Resource resource) {
        this.resource = resource;
    }

    @Autowired
    BranchConfiguration(Environment env, PeerGroup peerGroup) {
        this.isProduction = Arrays.asList(env.getActiveProfiles()).contains("prod");
        this.peerGroup = peerGroup;
    }

    @Bean
    BranchGroup branchGroup() throws IOException, IllegalAccessException, InstantiationException {
        this.branchGroup = new BranchGroup();
        BlockHusk genesis = Block.loadGenesis(resource.getInputStream());
        BlockChainBuilder builder = BlockChainBuilder.Builder()
                .addGenesis(genesis)
                .addContractId("4fc0d50cba2f2538d6cda789aa4955e88c810ef5");

        BlockChain blockChain = isProduction ? builder.buildForProduction() : builder.build();
        branchGroup.addBranch(blockChain.getBranchId(), blockChain, peerGroup, this);
        return branchGroup;
    }

    @Override
    public void onContractEvent(ContractEvent event) {
        log.warn("Deprecated");
    }
}
