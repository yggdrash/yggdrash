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

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.NodeManager;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
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

    private final boolean isSeedPeer;

    @Autowired
    public NodeScheduler(PeerGroup peerGroup, NodeManager nodeManager, NodeStatus nodeStatus) {
        this.peerGroup = peerGroup;
        this.nodeManager = nodeManager;
        this.nodeStatus = nodeStatus;
        this.isSeedPeer = nodeManager.isSeedPeer();
    }

    @Scheduled(cron = cronValue)
    public void healthCheck() {
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        peerGroup.healthCheck();
    }

    //TODO
    @Scheduled(cron = cronValue)
    public void channelRefresh() {

    }

    @Scheduled(cron = cronValue)
    public void consensusTask() {
        if (!nodeStatus.isUpStatus()) {
            log.debug("Waiting for up status...");
            return;
        }

        if (!isSeedPeer) {
            return;
        }

        // 시드피어 혼자 블록을 생성한다
        List<BranchId> branchIdList = nodeManager.getActiveBranchIdList();
        for (BranchId branchId : branchIdList) {
            nodeManager.generateBlock(branchId);
        }

        /*
        for (BranchId branchId : branchIdList) {
            //자신의 노드가 Seed 피어인지 확인한다.
            //현재 버전의 컨센서스 : Seed 피어면 피어를 지정하고, Seed 피어가 아니면 블록만 생성한다.

            if (nodeQueue.isEmpty()) {
                // Seed 피어는 nodeQueue 에 포함되지 않는다.
                nodeQueue.addAll(peerGroup.getPeerUriList(branchId));
            }
            String selectedPeerId = nodeQueue.poll();
            assert selectedPeerId != null;
            if (!peerGroup.getActivePeerList().isEmpty()) {
                log.debug("consensusTask | branchId => " + branchId);
                log.debug("consensusTask | selectedPeerId => " + selectedPeerId);
                Collection<PeerClientChannel> peerClientChannels
                        = peerGroup.getActivePeerListOf(branchId);
                // Seed 피어는 ActivePeer 에게 블록 생성할 피어(=selectedPeer)를 Broadcast 한다.
                for (PeerClientChannel peerClientChannel : peerClientChannels) {
                    peerClientChannel.broadcastConsensus(
                            branchId,
                            Peer.valueOf(selectedPeerId));
                }
            } else {
                nodeManager.generateBlock(branchId);
            }
        }
        */
    }
}
