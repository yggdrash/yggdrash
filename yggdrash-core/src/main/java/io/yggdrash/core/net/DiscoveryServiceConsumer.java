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

import java.util.List;

public class DiscoveryServiceConsumer implements DiscoveryConsumer {
    private final PeerTable peerTable;

    public DiscoveryServiceConsumer(PeerTable peerTable) {
        this.peerTable = peerTable;
    }

    @Override
    public List<Peer> findPeers(Peer target) {
        return peerTable.getClosestPeers(target, KademliaOptions.BUCKET_SIZE);
    }

    /*
    @Override
    public List<String> findPeers(Peer requestPeer) {
        return peerTable.getPeers(requestPeer);
    }
    */

    @Override
    public void afterFindPeersResponse() {
        // TODO remove cross connection
        /*
        try {
            if (!peerTable.isMaxHandler()) {
                peerTable.addHandler(peer);
            } else {
                // maxPeer 를 넘은경우부터 거리 계산된 peerTable 을 기반으로 peerChannel 업데이트
                if (peerTable.isClosePeer(peer)) {
                    log.warn("channel is max");
                    // TODO apply after test
                    //peerTable.reloadPeerChannel(new GRpcPeerHandler(peer));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to connect {} -> {}", peerTable.getOwner().toAddress(),
                    peer.toAddress());
        }
        */
    }

    @Override
    public String play(Peer from, String msg) {
        if ("Ping".equals(msg)) {
            peerTable.touchPeer(from);
            return "Pong";
        }
        return "";
    }
}
