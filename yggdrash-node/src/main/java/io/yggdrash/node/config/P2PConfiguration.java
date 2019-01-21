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
import io.yggdrash.core.net.Discovery;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.net.KademliaDiscovery;
import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerListener;
import io.yggdrash.core.net.PeerTable;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.GRpcPeerHandlerFactory;
import io.yggdrash.node.PeerTask;
import io.yggdrash.node.service.GRpcPeerListener;
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

    @Autowired
    P2PConfiguration(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Bean
    PeerHandlerFactory peerHandlerFactory() {
        return new GRpcPeerHandlerFactory();
    }

    @Bean
    PeerTable peerTable(Wallet wallet, StoreBuilder storeBuilder) {
        Peer owner = Peer.valueOf(wallet.getNodeId(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort());

        PeerStore peerStore = storeBuilder.buildPeerStore();
        PeerTable peerTable = new KademliaPeerTable(owner, peerStore);
        peerTable.setSeedPeerList(nodeProperties.getSeedPeerList());
        return peerTable;
    }

    @Bean
    PeerHandlerGroup peerHandlerGroup(PeerTable peerTable, PeerHandlerFactory peerHandlerFactory) {
        PeerHandlerGroup peerHandlerGroup = new PeerHandlerGroup(peerHandlerFactory);
        peerHandlerGroup.setPeerEventListener(peerTable);
        return peerHandlerGroup;
    }

    @Bean
    Discovery discovery(PeerTable peerTable) {
        return new KademliaDiscovery(peerTable);
    }

    @Bean
    DiscoveryConsumer discoveryConsumer(PeerTable peerTable) {
        return new DiscoveryServiceConsumer(peerTable);
    }

    @Bean
    BlockChainConsumer blockChainConsumer(BranchGroup branchGroup) {
        return new BlockChainServiceConsumer(branchGroup);
    }

    @Bean
    PeerListener peerListener(DiscoveryConsumer discoveryConsumer,
                              BlockChainConsumer blockChainConsumer) {
        PeerListener peerListener = new GRpcPeerListener();
        peerListener.initConsumer(discoveryConsumer, blockChainConsumer);
        return peerListener;
    }

    /**
     * Scheduling Beans
     */

    @Bean
    PeerTask peerTask() {
        return new PeerTask();
    }
}
