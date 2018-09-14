/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.PeerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class NodeHealthIndicator implements HealthIndicator {
    private final AtomicReference<Health> health = new AtomicReference<>(Health.down().build());

    private final DefaultConfig defaultConfig;

    private final BranchGroup branchGroup;

    private final PeerGroup peerGroup;

    @Autowired
    public NodeHealthIndicator(DefaultConfig defaultConfig, BranchGroup branchGroup,
                               PeerGroup peerGroup) {
        this.defaultConfig = defaultConfig;
        this.branchGroup = branchGroup;
        this.peerGroup = peerGroup;
    }

    @Override
    public Health health() {
        updateDetail(health.get().getStatus());
        return health.get();
    }

    public void up() {
        updateDetail(Status.UP);
    }

    public void sync() {
        updateDetail(new Status("SYNC", "Synchronizing.."));
    }

    private void updateDetail(Status status) {
        Health.Builder builder = Health.status(status);
        builder.withDetail("name", defaultConfig.getNodeName());
        builder.withDetail("version", defaultConfig.getNodeVersion());
        builder.withDetail("p2pVersion", defaultConfig.getNetworkP2PVersion());
        builder.withDetail("network", defaultConfig.getNetwork());
        // Add Node BranchIds and block index
        Map<BranchId, Long> branchs = branchGroup.getAllBranch()
                .stream()
                .collect(Collectors.toMap(BlockChain::getBranchId, BlockChain::getLastIndex));
        builder.withDetail("branchs", branchs);

        builder.withDetail("activePeers", peerGroup.getActivePeerList().size());
        health.set(builder.build());
    }
}
