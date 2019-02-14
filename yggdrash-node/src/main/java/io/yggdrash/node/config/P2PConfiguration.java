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

import io.yggdrash.core.akashic.SimpleSyncManager;
import io.yggdrash.core.akashic.SyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.core.net.BlockChainServiceConsumer;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.net.KademliaPeerNetwork;
import io.yggdrash.core.net.KademliaPeerTableGroup;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.net.PeerTableGroup;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.GRpcPeerHandlerFactory;
import io.yggdrash.node.PeerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NodeProperties.class)
public class P2PConfiguration {

    private final NodeProperties nodeProperties;
    private final StoreBuilder storeBuilder;

    @Autowired
    P2PConfiguration(NodeProperties nodeProperties, StoreBuilder storeBuilder) {
        this.nodeProperties = nodeProperties;
        this.storeBuilder = storeBuilder;
    }

    @Bean
    PeerHandlerFactory peerHandlerFactory() {
        return new GRpcPeerHandlerFactory();
    }

    @Bean
    PeerTableGroup peerTableGroup(Wallet wallet, PeerHandlerFactory peerHandlerFactory) {
        Peer owner = Peer.valueOf(wallet.getNodeId(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort(), nodeProperties.isSeed());
        PeerTableGroup peerTableGroup = new KademliaPeerTableGroup(owner, storeBuilder, peerHandlerFactory);
        peerTableGroup.setSeedPeerList(nodeProperties.getSeedPeerList());
        return peerTableGroup;
    }

    @Bean
    PeerHandlerGroup peerHandlerGroup(PeerTableGroup peerTableGroup, PeerHandlerFactory peerHandlerFactory) {
        PeerHandlerGroup peerHandlerGroup = new SimplePeerHandlerGroup(peerHandlerFactory);
        peerHandlerGroup.setPeerEventListener(peerTableGroup);
        return peerHandlerGroup;
    }

    @Bean
    PeerNetwork peerNetwork(PeerTableGroup peerTableGroup, PeerHandlerGroup peerHandlerGroup) {
        return new KademliaPeerNetwork(peerTableGroup, peerHandlerGroup);
    }

    @Bean
    DiscoveryConsumer discoveryConsumer(PeerTableGroup peerTableGroup) {
        return new DiscoveryServiceConsumer(peerTableGroup);
    }

    @Bean
    BlockChainConsumer blockChainConsumer(BranchGroup branchGroup) {
        return new BlockChainServiceConsumer(branchGroup);
    }

    @Bean
    SyncManager syncManager() {
        return new SimpleSyncManager();
    }

    /**
     * Scheduling Beans
     */

    @Bean
    PeerTask peerTask() {
        return new PeerTask();
    }
}
