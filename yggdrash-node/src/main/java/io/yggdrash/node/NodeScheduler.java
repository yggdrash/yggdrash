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

import io.yggdrash.core.net.NodeManager;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@EnableScheduling
class NodeScheduler {
    private static final Logger log = LoggerFactory.getLogger(NodeScheduler.class);

    private static final String cronValue = "*/10 * * * * *";

    private final Queue<String> nodeQueue = new LinkedBlockingQueue<>();

    private final NodeManager nodeManager;

    private final PeerGroup peerGroup;

    private final NodeStatus nodeStatus;

    @Autowired
    public NodeScheduler(PeerGroup peerGroup, NodeManager nodeManager,
                         NodeStatus nodeStatus) {
        this.peerGroup = peerGroup;
        this.nodeManager = nodeManager;
        this.nodeStatus = nodeStatus;
    }

    @Scheduled(fixedRate = 1000 * 10)
    public void healthCheck() {
        peerGroup.healthCheck();
    }

    @Scheduled(cron = cronValue)
    public void generateBlock() {
        if (!nodeStatus.isUpStatus()) {
            log.debug("Waiting for up status...");
            return;
        }

        if (nodeQueue.isEmpty()) {
            nodeQueue.addAll(peerGroup.getPeerUriList());
        }
        String peerId = nodeQueue.poll();
        assert peerId != null;
        if (peerGroup.getActivePeerList().isEmpty() || peerId.equals(nodeManager.getNodeUri())) {
            nodeManager.generateBlock();
        } else {
            log.debug("Skip generation by another " + peerId.substring(peerId.lastIndexOf("@")));
        }
    }
}
