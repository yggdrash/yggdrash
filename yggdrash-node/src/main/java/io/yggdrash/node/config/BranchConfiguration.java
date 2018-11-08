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
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockChainBuilder;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.genesis.BranchLoader;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.WebsocketSender;
import io.yggdrash.node.config.annotaion.EnableDefaultBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@EnableDefaultBranch
public class BranchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final boolean productionMode;

    BranchConfiguration(Environment env) {
        this.productionMode = Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    BranchLoader branchLoader(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender sender,
                              DefaultConfig defaultConfig) {
        BranchLoader loader = new BranchLoader(defaultConfig);
        for (GenesisBlock genesis : loader.getGenesisBlockList()) {
            addBranch(peerGroup, branchGroup, sender, genesis);
        }
        return loader;
    }

    private void addBranch(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender sender,
                           GenesisBlock genesis) {
        try {
            BlockChain branch = BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setProductMode(productionMode)
                    .build();
            branch.addListener(sender);
            branchGroup.addBranch(branch.getBranchId(), branch, peerGroup);
            peerGroup.addPeerTable(branch.getBranchId(), productionMode);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
