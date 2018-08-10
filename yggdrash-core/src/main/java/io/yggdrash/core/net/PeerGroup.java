/*
 * Copyright 2018 Akashic Foundation
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerGroup {

    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    private List<String> seedPeerList;

    public Peer addPeer(Peer peer) {
        if (peers.containsKey(peer.getYnodeUri())) {
            return null;
        }
        peers.put(peer.getYnodeUri(), peer);
        return peer;
    }

    public int count() {
        return peers.size();
    }

    public Peer removePeer(String peer) {
        return peers.remove(peer);
    }

    public Collection<Peer> getPeers() {
        return peers.values();
    }

    public boolean contains(String ynodeUri) {
        return peers.containsKey(ynodeUri);
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }

    public void clear() {
        this.peers.clear();
    }

    public List<String> getSeedPeerList() {
        return seedPeerList;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }
}
