/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.net;

import io.yggdrash.core.akashic.SyncManager;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BootStrapNode implements BootStrap {
    private static final Logger log = LoggerFactory.getLogger(BootStrapNode.class);

    private SyncManager syncManager;
    private NodeStatus nodeStatus;
    protected PeerNetwork peerNetwork;
    protected BranchGroup branchGroup;

    @Override
    public void bootstrapping() {
        try {
            nodeStatus.sync();
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                List<PeerHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());
                for (PeerHandler peerHandler : peerHandlerList) {
                    syncManager.syncBlock(peerHandler, blockChain);
                    syncManager.syncTransaction(peerHandler, blockChain);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            nodeStatus.up();
        }
    }

    public void setBranchGroup(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    protected void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void setPeerNetwork(PeerNetwork peerNetwork) {
        this.peerNetwork = peerNetwork;
    }

    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }
}
