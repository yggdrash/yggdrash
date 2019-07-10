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
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BlockChainSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManager.class);

    private NodeStatus nodeStatus;
    private BranchGroup branchGroup;
    private PeerNetwork peerNetwork;

    public BlockChainSyncManager(NodeStatus nodeStatus, PeerNetwork peerNetwork, BranchGroup branchGroup) {
        this.nodeStatus = nodeStatus;
        this.branchGroup = branchGroup;
        this.peerNetwork = peerNetwork;
    }

    @Override
    public void fullSync() {
        nodeStatus.sync();
        try {
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                List<BlockChainHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());

                fullSyncBlock(blockChain, peerHandlerList);

                syncTransaction(blockChain, peerHandlerList);
            }
        } finally {
            nodeStatus.up();
        }
    }

    @Override
    public boolean syncBlock(BlockChainHandler peerHandler, BlockChain blockChain) {
        long offset = blockChain.getBlockChainManager().getLastIndex() + 1;

        BranchId branchId = blockChain.getBranchId();
        Future<List<ConsensusBlock>> futureBlockList = peerHandler.syncBlock(branchId, offset);

        try {
            List<ConsensusBlock> blockList = futureBlockList.get();
            if (blockList.isEmpty()) {
                return true;
            }
            log.debug("[SyncManager] Synchronize reqStart={} receivedSize={}, receivedStart={} receivedEnd={} from={}",
                    offset,
                    blockList.size(),
                    ((ConsensusBlock) blockList.toArray()[0]).getBlock().getIndex(),
                    ((ConsensusBlock) blockList.toArray()[blockList.size() - 1]).getBlock().getIndex(),
                    peerHandler.getPeer().getYnodeUri());

            for (ConsensusBlock block : blockList) {
                blockChain.addBlock(block, false);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.debug("[SyncManager] Sync Block ERR occurred: {}", e.getMessage(), e);
        }

        return false;
    }

    private void fullSyncBlock(BlockChain blockChain, List<BlockChainHandler> peerHandlerList) {
        boolean retry = true;

        while (retry) {
            retry = false;
            for (BlockChainHandler peerHandler : peerHandlerList) {
                try {
                    boolean syncFinish = syncBlock(peerHandler, blockChain);
                    if (!syncFinish) {
                        retry = true;
                    }
                } catch (Exception e) {
                    String error = e.getMessage();
                    if (e.getCause() != null) {
                        error = e.getCause().getMessage();
                    }
                    log.warn("[SyncManager] Full Sync Block ERR occurred: {}, from={}", error,
                            peerHandler.getPeer().getYnodeUri());
                }
            }
        }
    }

    @Override
    public void syncTransaction(BlockChainHandler peerHandler, BlockChain blockChain) {
        Future<List<Transaction>> futureTxList = peerHandler.syncTx(blockChain.getBranchId());

        try {
            List<Transaction> txList = futureTxList.get();
            log.info("[SyncManager] Synchronize Tx receivedSize={}, from={}",
                    txList.size(), peerHandler.getPeer().getYnodeUri());
            addTransaction(blockChain, txList);

        } catch (InterruptedException | ExecutionException e) {
            log.debug("[SyncManager] Sync Tx ERR occurred: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void syncTransaction(BlockChain blockChain, List<BlockChainHandler> peerHandlerList) {

        for (BlockChainHandler peerHandler : peerHandlerList) {
            try {
                syncTransaction(peerHandler, blockChain);
            } catch (Exception e) {
                log.warn("[SyncManager] Sync Tx ERR occurred: {}", e.getCause().getMessage());
            }
        }
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

    // When broadcastBlock is called (BlockServiceConsumer)
    @Override
    public void catchUpRequest(ConsensusBlock block) {
        log.trace("catchUpRequest() block {}", block.getBlock().toJsonObject().toString());
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        if (blockChain == null) {
            log.trace("catchUpRequest(): blockChain is null. {}", block.getBlock().getBranchId().toString());
            return;
        }

        reqSyncBlockToHandlers(blockChain);

        try {
            blockChain.addBlock(block, false);
        } catch (Exception e) {
            log.warn("CatchUp block error={}", e.getMessage());
        }
    }

    // When syncBlock is called (BlockServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, long offset) {
        BlockChain blockChain = branchGroup.getBranch(branchId);
        if (blockChain == null) {
            log.trace("catchUpRequest(): blockChain is null. {}", branchId.toString());
            return;
        }

        reqSyncBlockToHandlers(blockChain);
    }

    // When ping received (DiscoveryServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, Peer from) {
        BlockChain blockChain = branchGroup.getBranch(branchId);
        if (blockChain == null) {
            log.trace("catchUpRequest(): blockChain is null. {}", branchId.toString());
            return;
        }

        long bestBlock = from.getBestBlock();
        long curBestBlock = blockChain.getBlockChainManager().getLastIndex();
        log.trace("catchUpRequest(): bestBlock from peer=({}), curBestBlock of branch=({})", bestBlock, curBestBlock);

        if (curBestBlock >= bestBlock) {
            return;
        }

        reqSyncBlockToHandlers(blockChain);
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
            log.debug("NodeStatus is down.");
            return;
        }
        BranchId branchId = blockChain.getBranchId();
        nodeStatus.sync();
        try {
            for (BlockChainHandler peerHandler : peerNetwork.getHandlerList(branchId)) {
                syncBlock(peerHandler, blockChain);
            }
        } finally {
            nodeStatus.up();
        }
    }
}