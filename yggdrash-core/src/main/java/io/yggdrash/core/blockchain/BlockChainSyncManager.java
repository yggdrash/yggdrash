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
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class BlockChainSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManager.class);

    private NodeStatus nodeStatus;
    private BranchGroup branchGroup;
    private PeerNetwork peerNetwork;
    private PeerTableGroup peerTableGroup;

    public BlockChainSyncManager(NodeStatus nodeStatus, PeerNetwork peerNetwork, BranchGroup branchGroup,
                                 PeerTableGroup peerTableGroup) {
        this.nodeStatus = nodeStatus;
        this.branchGroup = branchGroup;
        this.peerNetwork = peerNetwork;
        this.peerTableGroup = peerTableGroup;
    }

    @Override
    public boolean isSyncStatus() {
        return nodeStatus.isSyncStatus();
    }

    @Override
    public void fullSync() {
        nodeStatus.sync();
        try {
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                try {
                    List<BlockChainHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());
                    fullSyncBlock(blockChain, peerHandlerList);
                } finally {
                    blockChain.setFullSynced(true);
                    log.debug("Branch is fullSynced.");
                }
            }
        } finally {
            nodeStatus.up();
        }
    }

    @Override
    public boolean syncBlock(BlockChainHandler peerHandler, BlockChain blockChain) {
        long offset = blockChain.getBlockChainManager().getLastIndex() + 1;

        try {
            BranchId branchId = blockChain.getBranchId();
            Future<List<ConsensusBlock>> futureBlockList = peerHandler.syncBlock(branchId, offset);
            if (futureBlockList == null) {
                return false;
            }

            List<ConsensusBlock> blockList = futureBlockList.get();
            if (blockList.isEmpty()) {
                return false;
            }
            log.debug("syncBlock() reqBlock={} resBlock=({} - {}) from={}",
                    offset,
                    ((ConsensusBlock) blockList.toArray()[0]).getBlock().getIndex(),
                    ((ConsensusBlock) blockList.toArray()[blockList.size() - 1]).getBlock().getIndex(),
                    peerHandler.getPeer().getYnodeUri());

            for (ConsensusBlock block : blockList) {
                if (nodeStatus.isUpdateStatus()) {
                    log.debug("SyncBlock interrupted. Node is updating now...");
                    return false;
                }
                // Handling exception if the block was not added properly
                Map<String, List<String>> errorLogs = blockChain.addBlock(block, false);
                if (errorLogs.size() > 0) {
                    log.debug("addBlock() is failed. {}", errorLogs);
                    return false;
                }
            }

            if (blockChain.getBlockChainManager().getLastIndex() < peerHandler.getPeer().getBestBlock()) {
                log.debug("MyBlockIndex({}) < PeerBlockIndex({})",
                        blockChain.getBlockChainManager().getLastIndex(),
                        peerHandler.getPeer().getBestBlock());
                return syncBlock(peerHandler, blockChain);
            }
        } catch (Exception e) {
            log.info("syncBlock() is failed. {}", e.getMessage());
            return false;
        }

        return true;
    }

    private void fullSyncBlock(BlockChain blockChain, List<BlockChainHandler> peerHandlerList) {
        for (BlockChainHandler peerHandler : peerHandlerList) {
            try {
                peerHandler.getPeer().setBestBlock(
                        peerHandler.pingPong(blockChain.getBranchId(), peerTableGroup.getOwner(), "Ping"));
                long peerBestBlock = peerHandler.getPeer().getBestBlock();
                long lastBlockIndex = blockChain.getBlockChainManager().getLastIndex();
                long blockDiff = peerBestBlock - lastBlockIndex;
                if (peerBestBlock > lastBlockIndex) {
                    if (blockDiff > 10000) {
                        for (long l = 0; l < blockDiff / 10000L; l++) {
                            peerHandler.getPeer().setBestBlock(lastBlockIndex + (l + 1L) * 10000L);
                            syncBlock(peerHandler, blockChain);
                        }
                        peerHandler.getPeer().setBestBlock(peerBestBlock);
                    }
                    syncBlock(peerHandler, blockChain);
                }
            } catch (Exception e) {
                log.debug("fullSyncBlock() is failed. {}", e.getMessage());
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

        } catch (Exception e) {
            log.debug("[SyncManager] Sync Tx ERR occurred: {}", e.getMessage());
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
        try {
            log.trace("catchUpRequest() block {}", block.getBlock().toJsonObject().toString());
            BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
            reqSyncBlockToHandlers(blockChain);
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    }

    // When syncBlock is called (BlockServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, long offset) {
        try {
            BlockChain blockChain = branchGroup.getBranch(branchId);
            reqSyncBlockToHandlers(blockChain);
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    }

    // When ping received (DiscoveryServiceConsumer)
    @Override
    public void catchUpRequest(BranchId branchId, Peer from) {
        try {
            BlockChain blockChain = branchGroup.getBranch(branchId);
            long peerBlockIndex = from.getBestBlock();
            long myBlockIndex = blockChain.getBlockChainManager().getLastIndex();
            log.trace("catchUpRequest(): bestBlock from peer=({}), curBestBlock of branch=({})",
                    peerBlockIndex, myBlockIndex);

            if (myBlockIndex >= peerBlockIndex) {
                return;
            }

            reqSyncBlockToPeer(blockChain, from);
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    }

    private void addTransaction(BlockChain blockChain, List<Transaction> txList) {
        for (Transaction tx : txList) {
            try {
                blockChain.addTransaction(tx, false);
            } catch (Exception e) {
                log.warn("[SyncManager] Add Tx ERR occurred: {}", e.getMessage());
            }
        }
    }

    private void reqSyncBlockToHandlers(BlockChain blockChain) {
        if (nodeStatus.isSyncStatus()) {
            log.debug("NodeStatus is down. ({})", nodeStatus.toString());
            return;
        }

        if (nodeStatus.isUpdateStatus()) {
            log.debug("Node is updating now. ({})", nodeStatus.toString());
            return;
        }

        BranchId branchId = blockChain.getBranchId();
        nodeStatus.sync();
        try {
            for (BlockChainHandler peerHandler : peerNetwork.getHandlerList(branchId)) {
                syncBlock(peerHandler, blockChain);
            }
        } catch (Exception e) {
            log.warn("[SyncManager] Request sync block ERR occurred: {}", e.getMessage());
        } finally {
            nodeStatus.up();
        }
    }

    private void reqSyncBlockToPeer(BlockChain blockChain, Peer peer) {
        if (nodeStatus.isSyncStatus()) {
            log.debug("NodeStatus is down. ({})", nodeStatus.toString());
            return;
        }

        if (nodeStatus.isUpdateStatus()) {
            log.debug("Node is updating now. ({})", nodeStatus.toString());
            return;
        }

        nodeStatus.sync();
        try {
            syncBlock(peerNetwork.getPeerHandler(blockChain.getBranchId(), peer), blockChain);
        } catch (Exception e) {
            log.warn("[SyncManager] Request sync block ERR occurred: {}", e.getMessage());
        } finally {
            nodeStatus.up();
        }
    }

}