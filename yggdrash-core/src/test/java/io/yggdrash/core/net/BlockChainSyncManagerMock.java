/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockChainSyncManagerMock {
    static final BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
    static final NodeStatus nodeStatus = NodeStatusMock.mock;
    static final PeerTableGroup peerTableGroup = PeerTestUtils.createTableGroup();
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManagerMock.class);

    public static final BlockChainSyncManager mock = new BlockChainSyncManager(nodeStatus,
            PeerNetworkMock.mock, branchGroup, peerTableGroup);

    public BlockChainSyncManager getMock() {
        log.debug("branch size : {}", branchGroup.getAllBranch().size());
        log.debug("nodeStatus : {}", nodeStatus.isUpStatus());

        return new BlockChainSyncManager(nodeStatus, PeerNetworkMock.mock, branchGroup, peerTableGroup);
    }
}