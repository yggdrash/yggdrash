/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.blockchain;

import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.net.CatchUpSyncEventListener;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BlockChainSyncManager implements SyncManager, CatchUpSyncEventListener {
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManager.class);

    private NodeStatus nodeStatus;
    private BranchGroup branchGroup;
    private PeerNetwork peerNetwork;

    public BlockChainSyncManager(NodeStatus nodeStatus, PeerNetwork peerNetwork, BranchGroup branchGroup) {
        this.nodeStatus = nodeStatus;
        this.branchGroup = branchGroup;
        this.peerNetwork = peerNetwork;
    }

    /*
    The cases of doing (catch-up)sync :
    - When a new node connects to the network for the first time
    - When the node is restarted
    - When the height of the block received is higher
    - When the height of the block requested by the peer is higher
    - When the block height is the same for a certain period of time

    The following should be managed :
    - Request peer management (timeout etc)
    - Request syncBlock to multiple peers upon request
        - maxDiffBetweenCurrentAndReceivedBlockHeight = 100

    The current block synchronization proceeds recursively.
    That is, it recursively inquires whether the current block height is higher than
    the current block height.
    This should proceed in normal_mode of block synchronization.
    The synchronization will be very slow if the block height is high.

    [TODO]
    The "fast_sync" mode should be implemented so that when a new node is connected
    to the network(=branch) and quickly synchronize blocks that have already been verified,
    such as block-boxes or file types.
    If so, which node should receive the request for fast sync?
    A malicious node may transmit the blocks that have not been verified and DDoS attacks
    are possible with requests that require relatively large return values.
    Also, Isn't it necessary to request the current block height(=status)?
    The synchronization will take a long time when there are many gaps in block height during
    the node isn't connected to the network.
    */

    // When broadcastBlock is called (BlockChainServiceConsumer)
    @Override
    public void catchUpRequest(ConsensusBlock block) {
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        if (blockChain == null) {
            return;
        }

        reqSyncBlockToHandlers(blockChain);

        try {
            blockChain.addBlock(block, true);
        } catch (Exception e) {
            log.warn("Catch up block error={}", e.getMessage());
        }
    }

    // When syncBlock is called (BlockChainServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, long offset) {
        BlockChain blockChain = branchGroup.getBranch(branchId);
        if (blockChain == null) {
            return;
        }

        reqSyncBlockToHandlers(blockChain);
    }

    // When ping received (DiscoveryServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, Peer from) {
        BlockChain blockChain = branchGroup.getBranch(branchId);
        if (blockChain == null) {
            return;
        }

        long bestBlock = from.getBestBlock();
        long curBestBlock = blockChain.getLastIndex();

        if (curBestBlock >= bestBlock) {
            return;
        }

        reqSyncBlockToHandlers(blockChain);
    }

    @Override
    public boolean syncBlock(PeerHandler peerHandler, BlockChain blockChain) {
        long offset = blockChain.getLastIndex() + 1;

        BranchId branchId = blockChain.getBranchId();
        Future<List<ConsensusBlock>> futureBlockList = peerHandler.syncBlock(branchId, offset);

        try {
            List<ConsensusBlock> blockList = futureBlockList.get();
            if (blockList.isEmpty()) {
                return true;
            }
            log.info("[SyncManager] Synchronize block offset={} receivedSize={}, from={}",
                    offset, blockList.size(), peerHandler.getPeer().getYnodeUri());

            for (ConsensusBlock block : blockList) {
                blockChain.addBlock(block, false);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.debug("[SyncManager] Sync Block ERR occurred: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return false;
    }

    @Override
    public void syncTransaction(PeerHandler peerHandler, BlockChain blockChain) {
        Future<List<Transaction>> futureTxList = peerHandler.syncTx(blockChain.getBranchId());

        try {
            List<Transaction> txList = futureTxList.get();
            log.info("[SyncManager] Synchronize Tx receivedSize={}, from={}",
                    txList.size(), peerHandler.getPeer().getYnodeUri());
            addTransaction(blockChain, txList);

        } catch (InterruptedException | ExecutionException e) {
            log.debug("[SyncManager] Sync Tx ERR occurred: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void addTransaction(BlockChain blockChain, List<Transaction> txList) {
        for (Transaction tx : txList) {
            try {
                blockChain.addTransaction(tx);
            } catch (Exception e) {
                log.warn("[SyncManager] Add Tx ERR occurred: {}", e.getMessage());
            }
        }
    }

    private void reqSyncBlockToHandlers(BlockChain blockChain) {
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        BranchId branchId = blockChain.getBranchId();
        nodeStatus.sync();
        for (PeerHandler peerHandler : peerNetwork.getHandlerList(branchId)) {
            syncBlock(peerHandler, blockChain);
        }
        nodeStatus.up();
    }
}