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

package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.proto.Proto;

import java.util.Collections;
import java.util.List;

public class PeerHandlerMock implements PeerHandler {
    public static final PeerHandlerFactory factory = PeerHandlerMock::dummy;

    private final Peer peer;
    private boolean pongResponse = true;

    private PeerHandlerMock(String ynodeUri) {
        this.peer = Peer.valueOf(ynodeUri);
    }


    @Override
    public List<String> findPeers(Peer peer) {
        return null;
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
    }

    @Override
    public String ping(String message, Peer peer) {
        if (pongResponse) {
            pongResponse = false;
            return "Pong";
        }
        pongResponse = true;
        return null;
    }

    @Override
    public List<Proto.Block> syncBlock(BranchId branchId, long offset) {
        return Collections.singletonList(BlockChainTestUtils.genesisBlock().getInstance());
    }

    @Override
    public List<Proto.Transaction> syncTransaction(BranchId branchId) {
        Proto.Transaction protoTx = BlockChainTestUtils.createTransferTxHusk().getInstance();
        return Collections.singletonList(protoTx);
    }

    @Override
    public void broadcastTransaction(Proto.Transaction[] txs) {
    }

    @Override
    public void broadcastBlock(Proto.Block[] blocks) {
    }

    private static PeerHandler dummy(Peer peer) {
        return new PeerHandlerMock(peer.getYnodeUri());
    }
}