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

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.CatchUpSyncEventListener;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import static io.yggdrash.common.config.Constants.NODE_DISCOVERY_INITDELAY;
import static io.yggdrash.common.config.Constants.NODE_DISCOVERY_TIME;

public class PeerTask {

    private static final Logger log = LoggerFactory.getLogger(PeerTask.class);

    private PeerTableGroup peerTableGroup;
    private PeerDialer peerDialer;
    private CatchUpSyncEventListener listener;
    private PeerNetwork peerNetwork;

    @Autowired
    private BranchGroup branchGroup;

    @Autowired
    public void setPeerTableGroup(PeerTableGroup peerTableGroup) {
        this.peerTableGroup = peerTableGroup;
    }

    @Autowired
    public void setPeerDialer(PeerDialer peerDialer) {
        this.peerDialer = peerDialer;
    }

    @Autowired
    public void setListener(CatchUpSyncEventListener listener) {
        this.listener = listener;
    }

    @Autowired
    public void setPeerNetwork(PeerNetwork peerNetwork) {
        this.peerNetwork = peerNetwork;
    }

    @Scheduled(initialDelay = NODE_DISCOVERY_INITDELAY, fixedRate = NODE_DISCOVERY_TIME)
    public void healthCheck() {
        try {
            for (BranchId branchId : peerTableGroup.getAllBranchId()) {
                for (PeerHandler peerHandler : peerNetwork.getHandlerList(branchId)) {
                    long peerBlockIndex
                            = peerDialer.healthCheck(branchId, peerTableGroup.getOwner(), peerHandler.getPeer());
                    log.trace("HealthCheck() Node({}) BlockIndex({})",
                            peerHandler.getPeer().getYnodeUri(), peerBlockIndex);
                    if (peerBlockIndex >= 0) {
                        peerHandler.getPeer().setBestBlock(peerBlockIndex);
                        if (branchGroup != null && peerBlockIndex > branchGroup.getLastIndex(branchId)) {
                            listener.catchUpRequest(branchId, peerHandler.getPeer());
                        }
                    } else {
                        peerHandler.setFailCount(peerHandler.getFailCount() + 1);
                        if (peerHandler.getFailCount() > 3) {
                            peerTableGroup.dropPeer(branchId, peerHandler.getPeer());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("healthCheck() is failed. {}", e.getMessage());
        }
    }

    // refresh performs a lookup for a random target to keep buckets full.
    // seed nodes are inserted if the table is empty (initial bootstrap or discarded faulty peers).
    @Scheduled(cron = "*/" + KademliaOptions.BUCKET_REFRESH + " * * * * *")
    public void refresh() {
        try {
            peerTableGroup.refresh();
            log.debug("refresh(): {}", peerTableGroup.getActivePeerListWithStatus());
        } catch (Exception e) {
            log.warn("refresh() is failed. {}", e.getMessage());
        }
    }

}
