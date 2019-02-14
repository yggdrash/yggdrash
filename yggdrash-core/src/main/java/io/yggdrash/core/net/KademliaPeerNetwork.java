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

import java.util.List;

public class KademliaPeerNetwork implements PeerNetwork {

    private PeerTableGroup peerTableGroup;

    private PeerHandlerGroup peerHandlerGroup;

    public KademliaPeerNetwork(PeerTableGroup peerTableGroup, PeerHandlerGroup peerHandlerGroup) {
        this.peerTableGroup = peerTableGroup;
        this.peerHandlerGroup = peerHandlerGroup;
    }

    @Override
    public void init(BranchId branchId) {
        peerTableGroup.createTable(branchId);
        peerTableGroup.selfRefresh();
        List<Peer> closestPeerList =
                peerTableGroup.getClosestPeers(branchId, peerTableGroup.getOwner(), KademliaOptions.BROADCAST_SIZE);
        for (Peer peer : closestPeerList) {
            peerHandlerGroup.healthCheck(branchId, peerTableGroup.getOwner(), peer);
        }
    }

    @Override
    public List<PeerHandler> getHandlerList(BranchId branchId) {
        List<Peer> peerList = peerTableGroup.getBroadcastPeerList(branchId);
        return peerHandlerGroup.getHandlerList(peerList);
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        List<PeerHandler> getHandlerList = getHandlerList(tx.getBranchId());
        for (PeerHandler peerHandler : getHandlerList) {
            try {
                peerHandler.broadcastTransaction(tx);
            } catch (Exception e) {
                peerHandlerGroup.removeHandler(peerHandler);
            }
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        List<PeerHandler> getHandlerList = getHandlerList(block.getBranchId());
        for (PeerHandler peerHandler : getHandlerList) {
            try {
                peerHandler.broadcastBlock(block);
            } catch (Exception e) {
                peerHandlerGroup.removeHandler(peerHandler);
            }
        }
    }
}
