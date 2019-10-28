/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.p2p;

import io.grpc.ConnectivityState;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class PeerHandlerMock implements BlockChainHandler {
    private static final Logger log = LoggerFactory.getLogger(PeerHandlerMock.class);
    private static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";
    private static final Peer OWNER = Peer.valueOf(NODE_URI_PREFIX + 32920);

    public static final BlockChainHandlerFactory factory = PeerHandlerMock::dummy;

    private final Peer peer;
    private boolean pongResponse = true;
    private int failCount = 0;

    private PeerHandlerMock(String ynodeUri) {
        this.peer = Peer.valueOf(ynodeUri);
    }

    @Override
    public List<Peer> findPeers(BranchId branchId, Peer peer) {
        List<Peer> result = new ArrayList<>();

        if (OWNER.equals(peer)) {
            for (int port = 32921; port < 32927; port++) { // return 6 peers
                result.add(Peer.valueOf(NODE_URI_PREFIX + port));
            }
        } else {
            for (int port = 32950; port < 32955; port++) { // return 5 peers
                result.add(Peer.valueOf(NODE_URI_PREFIX + port));
            }
        }
        return result;
    }

    @Override
    public String gerConnectivityState() {
        return ConnectivityState.CONNECTING.toString();
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
    }

    @Override
    public String ping(BranchId branchId, Peer owner, String message) {
        if (pongResponse) {
            pongResponse = false;
            return "Pong";
        }
        pongResponse = true;
        return null;
    }

    @Override
    public long pingPong(BranchId branchId, Peer owner, String message) {
        if (pongResponse) {
            pongResponse = false;
            return 1;
        }
        pongResponse = true;
        return -1;
    }

    @Override
    public Future<List<ConsensusBlock>> syncBlock(BranchId branchId, long offset) {
        log.debug("[PeerHandlerMock] SyncBlock branchId={}, offset={}", branchId, offset);

        CompletableFuture<List<ConsensusBlock>> future = new CompletableFuture<>();

        List<ConsensusBlock> tmp = new ArrayList<>();
        if (offset < 33) {
            for (int i = (int) offset; i < (int) offset + 33; i++) {
                tmp.add(BlockChainTestUtils.getSampleBlockList().get(i - 1));
            }
        }
        future.complete(tmp);
        return future;
    }

    @Override
    public Future<List<Transaction>> syncTx(BranchId branchId) {
        log.debug("[PeerHandlerMock] SyncTx branchId={}", branchId);

        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();
        future.complete(Collections.singletonList(BlockChainTestUtils.createTransferTx()));

        return future;
    }

    @Override
    public void broadcastBlock(ConsensusBlock block) {

    }

    @Override
    public void broadcastTx(Transaction tx) {
    }

    @Override
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    @Override
    public int getFailCount() {
        return failCount;
    }

    public static BlockChainHandler dummy() {
        return dummy(null, OWNER);
    }

    public static BlockChainHandler dummy(String consensus, Peer peer) {
        return new PeerHandlerMock(peer.getYnodeUri());
    }

}
