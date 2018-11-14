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

import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P2PConfiguration {

    private final NodeProperties nodeProperties;

    @Autowired
    P2PConfiguration(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Bean
    PeerGroup peerGroup(Wallet wallet) {
        Peer owner = Peer.valueOf(wallet.getNodeId(), nodeProperties.getGrpc().getHost(),
                nodeProperties.getGrpc().getPort());
        PeerGroup peerGroup = new PeerGroup(owner, nodeProperties.getMaxPeers());
        peerGroup.setSeedPeerList(nodeProperties.getSeedPeerList());
        return peerGroup;
    }
}
