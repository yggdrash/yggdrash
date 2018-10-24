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

package io.yggdrash.node.config.annotaion;

import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockChainBuilder;
import io.yggdrash.core.Branch;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.WebsocketSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class DefaultBranchAutoConfig {
    private final boolean productionMode;

    @Value("classpath:/genesis-stem.json")
    Resource stemResource;

    @Value("classpath:/genesis-yeed.json")
    Resource yeedResource;

    DefaultBranchAutoConfig(Environment env) {
        this.productionMode = Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    @Bean(Branch.STEM)
    BlockChain stem(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IOException, IllegalAccessException, InstantiationException {
        return addBranch(stemResource.getInputStream(), peerGroup, branchGroup,
                websocketSender);
    }

    @Bean(Branch.YEED)
    BlockChain yeed(PeerGroup peerGroup, BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IOException, IllegalAccessException, InstantiationException {
        return addBranch(yeedResource.getInputStream(), peerGroup, branchGroup,
                websocketSender);
    }

    private BlockChain addBranch(InputStream json, PeerGroup peerGroup,
                                 BranchGroup branchGroup, WebsocketSender websocketSender)
            throws IllegalAccessException, InstantiationException {
        GenesisBlock genesis = new GenesisBlock(json);

        BlockChain branch = BlockChainBuilder.Builder()
                .addGenesis(genesis)
                .setProductMode(productionMode)
                .build();
        branch.addListener(websocketSender);
        branchGroup.addBranch(branch.getBranchId(), branch, peerGroup);
        return branch;
    }
}
