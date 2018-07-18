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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeerGroup {

    private List<Peer> peers = Collections.synchronizedList(new ArrayList<>());

    private List<String> seedPeerList;

    public void addPeer(Peer peer) {
        if (!peers.contains(peer)) {
            peers.add(peer);
        }
    }

    public List<Peer> getPeers() {
        return peers;
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
