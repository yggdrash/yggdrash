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
import io.yggdrash.core.net.Discovery;
import io.yggdrash.core.net.KademliaDiscovery;
import io.yggdrash.core.net.PeerListener;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.core.store.PeerStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.GRpcClientChannel;
import io.yggdrash.node.PeerTask;
import io.yggdrash.node.service.BlockChainService;
import io.yggdrash.node.service.GRpcPeerListener;
import io.yggdrash.node.service.PeerService;
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
    PeerGroup peerGroup(Wallet wallet, StoreBuilder storeBuilder) {
        Peer owner = Peer.valueOf(wallet.getNodeId(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort());

        PeerStore peerStore = storeBuilder.buildPeerStore();
        PeerGroup peerGroup = new PeerGroup(owner, peerStore, nodeProperties.getMaxPeers());
        peerGroup.setSeedPeerList(nodeProperties.getSeedPeerList());
        return peerGroup;
    }

    @Bean
    Discovery discovery(PeerGroup peerGroup) {
        Discovery discovery = new KademliaDiscovery() {
            @Override
            public PeerClientChannel getClient(Peer peer) {
                return new GRpcClientChannel(peer);
            }
        };
        discovery.setPeerGroup(peerGroup);
        return discovery;
    }

    @Bean
    PeerListener peerListener(PeerGroup peerGroup, BranchGroup branchGroup) {
        GRpcPeerListener server = new GRpcPeerListener();
        server.addService(new PeerService(peerGroup));
        server.addService(new BlockChainService(branchGroup));
        return server;
    }

    /**
     * Scheduling Beans
     */

    @Bean
    PeerTask peerTask() {
        return new PeerTask();
    }
}
