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
import io.yggdrash.core.genesis.BranchJson;
import io.yggdrash.core.genesis.BranchLoader;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.WebsocketSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Configuration
public class BranchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final boolean productionMode;

    @Value("classpath:/branch-stem.json")
    Resource stemResource;

    @Value("classpath:/branch-yeed.json")
    Resource yeedResource;

    @Value("classpath:/branch-sw.json")
    Resource swResource;

    BranchConfiguration(Environment env) {
        this.productionMode = Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    @Bean("stem")
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain stem(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IOException {
        BlockChain blockChain = addBranch(stemResource.getInputStream(), peerGroup, branchGroup,
                websocketSender);
        websocketSender.setStemBranchId(blockChain.getBranchId());
        return blockChain;
    }

    @Bean("yeed")
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain yeed(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IOException {
        return addBranch(yeedResource.getInputStream(), peerGroup, branchGroup,
                websocketSender);
    }

    @Bean("sw")
    @ConditionalOnProperty("yggdrash.branch.default.active")
    BlockChain none(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IOException {
        return addBranch(swResource.getInputStream(), peerGroup, branchGroup,
                websocketSender);
    }

    @Bean
    BranchGroup branchGroup() {
        return new BranchGroup();
    }

    @Bean
    BranchLoader branchLoader(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender sender,
                              DefaultConfig defaultConfig) {
        try {
            BranchLoader loader = new BranchLoader(defaultConfig.getBranchPath());
            for (GenesisBlock genesis : loader.getGenesisBlockList()) {
                addBranch(genesis, peerGroup, branchGroup, sender);
            }
            return loader;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private BlockChain addBranch(InputStream is, PeerGroup peerGroup, BranchGroup branchGroup,
                                 WebsocketSender sender) throws IOException {
        BranchJson branchJson = BranchJson.toBranchJson(is);
        GenesisBlock genesis = new GenesisBlock(branchJson);

        return addBranch(genesis, peerGroup, branchGroup, sender);
    }

    private BlockChain addBranch(GenesisBlock genesis, PeerGroup peerGroup, BranchGroup branchGroup,
                                 WebsocketSender sender) {
        BlockChain branch = BlockChainBuilder.Builder()
                .addGenesis(genesis)
                .setProductMode(productionMode)
                .build();
        branch.addListener(sender);
        branchGroup.addBranch(branch.getBranchId(), branch, peerGroup);
        peerGroup.addPeerTable(branch.getBranchId(), productionMode);
        return branch;
    }

}
