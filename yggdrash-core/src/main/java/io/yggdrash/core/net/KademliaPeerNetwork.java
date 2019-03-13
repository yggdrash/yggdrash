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

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class KademliaPeerNetwork implements PeerNetwork {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerNetwork.class);

    private final BlockingQueue<TransactionHusk> txQueue = new LinkedBlockingQueue<>();
    private final ExecutorService txExecutor = Executors.newSingleThreadExecutor();

    private final BlockingQueue<BlockHusk> blockQueue = new LinkedBlockingQueue<>();
    private final ExecutorService blockExecutor = Executors.newSingleThreadExecutor();

    private final PeerTableGroup peerTableGroup;

    private final PeerDialer peerDialer;

    public KademliaPeerNetwork(PeerTableGroup peerTableGroup, PeerDialer peerDialer) {
        this.peerTableGroup = peerTableGroup;
        this.peerDialer = peerDialer;
    }

    @Override
    public void init() {
        log.info("Init node={}", peerTableGroup.getOwner().toString());
        peerTableGroup.selfRefresh();

        for (BranchId branchId : peerTableGroup.getAllBranchId()) {
            List<Peer> closestPeerList =
                    peerTableGroup.getClosestPeers(branchId, peerTableGroup.getOwner(), KademliaOptions.BROADCAST_SIZE);
            for (Peer peer : closestPeerList) {
                peerDialer.healthCheck(branchId, peerTableGroup.getOwner(), peer);
            }
        }
        txExecutor.execute(new TxWorker(txQueue));
        blockExecutor.execute(new BlockWorker(blockQueue));
    }

    @Override
    public void destroy() {
        peerDialer.destroyAll();
        txExecutor.shutdown();
        blockExecutor.shutdown();
    }

    @Override
    public void addNetwork(BranchId branchId) {
        peerTableGroup.createTable(branchId);
    }

    @Override
    public List<PeerHandler> getHandlerList(BranchId branchId) {
        List<Peer> peerList = peerTableGroup.getBroadcastPeerList(branchId);
        return peerDialer.getHandlerList(peerList);
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        try {
            txQueue.put(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) { //TODO AddBlock BP
        try {
            blockQueue.put(block);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    private class TxWorker implements Runnable {
        private final BlockingQueue<TransactionHusk> queue;

        TxWorker(BlockingQueue<TransactionHusk> queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                while (!txExecutor.isTerminated()) {
                    TransactionHusk tx = queue.take();
                    List<PeerHandler> getHandlerList = getHandlerList(tx.getBranchId());
                    for (PeerHandler peerHandler : getHandlerList) {
                        try {
                            peerHandler.broadcastTx(tx);
                        } catch (Exception e) {
                            log.warn("[KademliaPeerNetwork] broadcast {} -> {}, tx ERR: {}",
                                    peerTableGroup.getOwner().getPort(),
                                    peerHandler.getPeer().getPort(), e.getMessage());
                            peerDialer.removeHandler(peerHandler);
                        }
                    }
                }
            } catch (InterruptedException e) {
                txExecutor.shutdown();
            }
        }
    }

    private class BlockWorker implements Runnable {
        private final BlockingQueue<BlockHusk> queue;

        BlockWorker(BlockingQueue<BlockHusk> queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                while (!blockExecutor.isTerminated()) {
                    BlockHusk block = queue.take();
                    List<PeerHandler> getHandlerList = getHandlerList(block.getBranchId());
                    for (PeerHandler peerHandler : getHandlerList) { //TODO Verify NULL
                        try {
                            peerHandler.broadcastBlock(block);
                        } catch (Exception e) {
                            log.warn("[KademliaPeerNetwork] broadcast block ERR: {}", e.getMessage());
                            peerDialer.removeHandler(peerHandler);
                        }
                    }
                }
            } catch (InterruptedException e) {
                blockExecutor.shutdown();
            }
        }
    }
}
