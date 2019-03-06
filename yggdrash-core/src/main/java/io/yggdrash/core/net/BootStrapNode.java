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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BootStrapNode implements BootStrap, CatchUpSyncEventListener {
    private static final Logger log = LoggerFactory.getLogger(BootStrapNode.class);

    private SyncManager syncManager;
    private NodeStatus nodeStatus;
    private PeerNetwork peerNetwork;
    protected BranchGroup branchGroup;

    @Override
    public void bootstrapping() {
        peerNetwork.init();
        try {
            nodeStatus.sync();
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                List<PeerHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());
                for (PeerHandler peerHandler : peerHandlerList) {
                    syncManager.syncBlock(peerHandler, blockChain, -1);
                    syncManager.syncTransaction(peerHandler, blockChain);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            nodeStatus.up();
        }
    }

    @Override
    public void catchUpRequest(BlockHusk block) {
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        if (blockChain == null) {
            return;
        }
        List<PeerHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());
        nodeStatus.sync();
        for (PeerHandler peerHandler : peerHandlerList) {
            syncManager.syncBlock(peerHandler, blockChain, block.getIndex());
        }
        nodeStatus.up();
        try {
            blockChain.addBlock(block, true);
        } catch (Exception e) {
            log.warn("Catch up block error={}", e.getMessage());
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
