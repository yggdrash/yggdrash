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

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandlerMock;

import java.util.ArrayList;
import java.util.List;

public class PeerNetworkMock implements PeerNetwork {
    public static final PeerNetwork mock = new PeerNetworkMock();

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public List<BlockChainHandler> getHandlerList(BranchId branchId) {
        List<BlockChainHandler> peerHandlerList = new ArrayList<>();
        for (int port = 32919; port < 32922; port++) {
            peerHandlerList.add(PeerHandlerMock.dummy(null, Peer.valueOf("ynode://75bff16c@127.0.0.1:" + port)));
        }
        return peerHandlerList;
    }

    @Override
    public BlockChainHandler getPeerHandler(BranchId branchId, Peer peer) {
        return PeerHandlerMock.dummy(null, Peer.valueOf("ynode://75bff16c@127.0.0.1:32930"));
    }

    @Override
    public void receivedTransaction(Transaction tx) {
    }

    @Override
    public void chainedBlock(ConsensusBlock block) {
    }
}
