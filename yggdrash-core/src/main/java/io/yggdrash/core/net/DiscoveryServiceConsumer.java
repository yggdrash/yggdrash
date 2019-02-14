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

import io.yggdrash.core.blockchain.BranchId;

import java.util.List;

public class DiscoveryServiceConsumer implements DiscoveryConsumer {
    private final PeerTableGroup peerTableGroup;

    public DiscoveryServiceConsumer(PeerTableGroup peerTableGroup) {
        this.peerTableGroup = peerTableGroup;
    }

    @Override
    public List<Peer> findPeers(BranchId branchId, Peer target) {
        return peerTableGroup.getClosestPeers(branchId, target, KademliaOptions.BUCKET_SIZE);
    }

    @Override
    public String ping(BranchId branchId, Peer from, Peer to, String msg) {
        //TODO Consider adding expiration time
        if ("Ping".equals(msg) && peerTableGroup.getOwner().toAddress().equals(to.toAddress())) {
            peerTableGroup.addPeer(branchId, from);
            return "Pong";
        }
        return "";
    }
}
