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

import java.util.List;
import java.util.Map;

public interface PeerTable extends PeerEventListener {

    Peer getOwner();

    List<Peer> getBootstrappingSeedList();

    Peer addPeer(Peer peer);

    int count();

    List<Peer> getClosestPeers(int maxPeers); // getNeighbor

    void setSeedPeerList(List<String> seedPeerList);

    List<String> getPeers(Peer peer);

    List<String> getPeerUriList();

    List<Peer> getLatestPeers(long reqTime);

    Map<Integer, List<Peer>> getBucketIdAndPeerList(); //for debugging

    List<String> getAllPeersFromBucketsOf(); //for debugging

    void touchPeer(Peer peer);
}
