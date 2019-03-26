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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.SyncManager;
import io.yggdrash.core.net.KademliaPeerNetwork;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.node.PeerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableScheduling
@DependsOn("branchLoader")
public class NetworkConfiguration {

    private final NodeProperties nodeProperties;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    BlockChain yggdrash;

    public NetworkConfiguration(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    public void setYggdrash(BlockChain yggdrash) {
        this.yggdrash = yggdrash;
    }

    @Bean
    public PeerNetwork peerNetwork(PeerTableGroup peerTableGroup, PeerDialer peerDialer, BranchGroup branchGroup) {
        KademliaPeerNetwork peerNetwork = new KademliaPeerNetwork(peerTableGroup, peerDialer);
        for (BlockChain blockChain : branchGroup.getAllBranch()) {
            blockChain.addListener(peerNetwork);
            peerNetwork.addNetwork(blockChain.getBranchId());
            if (nodeProperties.isDelivery() && yggdrash != null) {
                peerNetwork.setValidator(yggdrash.getBranchId(), parseValidator(blockChain.getBranch().getConsensus()));
            }
        }
        return peerNetwork;
    }

    /**
     * Scheduling Beans
     */
    @Bean
    PeerTask peerTask() {
        return new PeerTask();
    }

    @Bean
    public SyncManager syncManager(NodeStatus nodeStatus, PeerNetwork peerNetwork, BranchGroup branchGroup) {
        return new BlockChainSyncManager(nodeStatus, peerNetwork, branchGroup);
    }

    private List<Peer> parseValidator(JsonObject consensus) {
        List<Peer> validatorList = new ArrayList<>();
        Set<Map.Entry<String, JsonElement>> entrySet = consensus.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            Peer peer = Peer.valueOf(entry.getKey(),
                    entry.getValue().getAsJsonObject().get("host").getAsString(),
                    entry.getValue().getAsJsonObject().get("port").getAsInt());
            validatorList.add(peer);
        }
        return validatorList;
    }

}
