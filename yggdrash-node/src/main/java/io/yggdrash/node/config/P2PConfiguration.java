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
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.p2p.BlockChainDialer;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.core.p2p.PeerTableGroupBuilder;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.service.PeerHandlerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(P2PConfiguration.class);

    private final NodeProperties nodeProperties;

    @Autowired
    P2PConfiguration(NodeProperties nodeProperties, Environment env) {
        this.nodeProperties = nodeProperties;
        boolean isLocal = Arrays.asList(env.getActiveProfiles()).contains("local");
        if (!isLocal && "localhost".equals(nodeProperties.getGrpc().getHost())) {
            try {
                nodeProperties.getGrpc().setHost(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

    @Bean
    PeerDialer peerDialer() {
        return new BlockChainDialer(PeerHandlerProvider.factory());
    }

    @Bean
    Peer owner(Wallet wallet) {
        return Peer.valueOf(wallet.getHexAddress(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort(), nodeProperties.isSeed());
    }

    @Bean
    PeerTableGroup peerTableGroup(Peer owner, PeerDialer peerDialer, DefaultConfig defaultConfig) {
        StoreBuilder storeBuilder = StoreBuilder.newBuilder().setConfig(defaultConfig);
        return PeerTableGroupBuilder.newBuilder()
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
}
