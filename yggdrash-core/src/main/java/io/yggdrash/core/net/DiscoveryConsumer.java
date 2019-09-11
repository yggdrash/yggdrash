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
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface DiscoveryConsumer {

    void setListener(CatchUpSyncEventListener listener);

    List<Peer> findPeers(BranchId branchId, Peer target);

    String ping(BranchId branchId, Peer from, Peer to, String msg);

    Proto.Pong ping(BranchId branchId, Peer from, Peer to, String msg, long blockIndex, boolean normalHost);
}
