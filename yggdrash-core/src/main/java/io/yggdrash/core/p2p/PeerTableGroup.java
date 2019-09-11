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

package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BranchId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PeerTableGroup extends PeerEventListener {

    void setSeedPeerList(List<String> seedPeerList);

    List<String> getSeedPeerList();

    PeerTable createTable(BranchId branchId);

    Set<BranchId> getAllBranchId();

    PeerTable getPeerTable(BranchId branchId);

    Peer getOwner();

    void addPeer(BranchId branchId, Peer peer);

    void dropPeer(BranchId branchId, Peer peer);

    boolean contains(BranchId branchId);

    Map<String, String> getActivePeerListWithStatus();

    List<Peer> getClosestPeers(BranchId branchId, Peer targetPeer, int limit);

    List<Peer> getBroadcastPeerList(BranchId branchId);

    void copyLiveNode();

    void selfRefresh();

    void refresh();
}
