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

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.core.net.BlockChainServiceConsumer;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerFactory;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.core.p2p.PeerTableGroupBuilder;
import io.yggdrash.core.p2p.SimplePeerDialer;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.GRpcPeerHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(NodeProperties.class)
public class P2PConfiguration {

    private final NodeProperties nodeProperties;
    private final StoreBuilder storeBuilder;

    @Autowired
    P2PConfiguration(NodeProperties nodeProperties, StoreBuilder storeBuilder, Environment env) {
        this.nodeProperties = nodeProperties;
        this.storeBuilder = storeBuilder;
        boolean isLocal = Arrays.asList(env.getActiveProfiles()).contains("local");
        if (!isLocal && "localhost".equals(nodeProperties.getGrpc().getHost())) {
            try {
                nodeProperties.getGrpc().setHost(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Bean
    PeerHandlerFactory peerHandlerFactory() {
        return new GRpcPeerHandlerFactory();
    }

    @Bean
    PeerDialer peerDialer(PeerHandlerFactory peerHandlerFactory) {
        return new SimplePeerDialer(peerHandlerFactory);
    }

    @Bean
    Peer owner(Wallet wallet) {
        return Peer.valueOf(wallet.getHexAddress(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort(), nodeProperties.isSeed());
    }

    @Bean
    PeerTableGroup peerTableGroup(Peer owner, PeerDialer peerDialer) {

        return PeerTableGroupBuilder.Builder()
                .setOwner(owner)
                .setStoreBuilder(storeBuilder)
                .setPeerDialer(peerDialer)
                .setSeedPeerList(nodeProperties.getSeedPeerList())
                .build();
    }

    @Bean
    DiscoveryConsumer discoveryConsumer(PeerTableGroup peerTableGroup) {
        return new DiscoveryServiceConsumer(peerTableGroup);
    }

    @Bean
    BlockChainConsumer blockChainConsumer(BranchGroup branchGroup) {
        return new BlockChainServiceConsumer(branchGroup);
    }
}
