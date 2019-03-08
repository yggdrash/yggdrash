/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerTable;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public class PeerTask {

    private static final Logger log = LoggerFactory.getLogger(PeerTask.class);

    private PeerTableGroup peerTableGroup;
    private PeerDialer peerDialer;
    private NodeStatus nodeStatus;

    @Autowired
    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    @Autowired
    public void setPeerTableGroup(PeerTableGroup peerTableGroup) {
        this.peerTableGroup = peerTableGroup;
    }

    @Autowired
    public void setPeerDialer(PeerDialer peerDialer) {
        this.peerDialer = peerDialer;
    }

    @Scheduled(fixedRate = 10000)
    public void healthCheck() { // ==> Task of PeerDialer?
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        for (BranchId branchId : peerTableGroup.getAllBranchId()) {
            PeerTable peerTable = peerTableGroup.getPeerTable(branchId);
            List<Peer> closestPeerList =
                    peerTable.getClosestPeers(peerTableGroup.getOwner(), KademliaOptions.BROADCAST_SIZE);
            //log.trace("peerTask  :: healthCheck => size={}, branch={}", closestPeerList.size(), branchId);
            closestPeerList.forEach(peer -> peerDialer.healthCheck(branchId, peerTableGroup.getOwner(), peer));
        }
    }

    // refresh performs a lookup for a random target to keep buckets full.
    // seed nodes are inserted if the table is empty (initial bootstrap or discarded faulty peers).
    @Scheduled(cron = "*/" + KademliaOptions.BUCKET_REFRESH + " * * * * *")
    public void refresh() {
        peerTableGroup.refresh();
    }

    // revalidate checks that the last node in a random bucket is still live
    // and replaces or deletes the node if it isn't
    //@Scheduled(cron = "*/10 * * * * *")
    public void revalidate() {
        for (BranchId branchId : peerTableGroup.getAllBranchId()) {
            PeerTable peerTable = peerTableGroup.getPeerTable(branchId);

            Peer last = peerTable.peerToRevalidate();
            if (last == null || last.equals(peerTableGroup.getOwner())) {
                return;
            }
            // Ping the selected node and wait for a pong (set last.id to ping msg)
            //log.debug("[revalidate] last ynodeUri => " + last.getYnodeUri());
            if (peerDialer.healthCheck(branchId, peerTableGroup.getOwner(), last)) {
                // The peer responded, move it to the front
                peerTable.getBucketByPeer(last).bump(last);
            } else {
                // No reply received, pick a replacement or delete the node
                // if there aren't any replacement
                Peer result = peerTable.pickReplacement(last);
                if (result != null) {
                    log.debug("Replaced dead peer '{}' to '{}'",
                            last.getPeerId(), result.getPeerId());
                } else {
                    log.debug("Removed dead peer '{}'", last.getPeerId());
                }
            }
        }
    }

    // copyNode adds peers from the table to the database if they have been in the table
    // longer then minTableTime.
    @Scheduled(cron = "*/30 * * * * *")
    public void copyLiveNode() {
        peerTableGroup.copyLiveNode();
    }

    public PeerDialer getPeerDialer() { //For Test
        return peerDialer;
    }
}
