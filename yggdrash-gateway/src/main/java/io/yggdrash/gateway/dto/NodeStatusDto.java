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

package io.yggdrash.gateway.dto;

import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.PeerTableGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NodeStatusDto {

    public String nodeName;
    public String nodeVersion;
    public List<StatusDto> status;
    public Map<String, String> activePeerList; // <peerUri : connectivityState>

    public static NodeStatusDto createBy(BranchGroup branchGroup, PeerTableGroup peerTableGroup) {
        NodeStatusDto nodeStatusDto = new NodeStatusDto();
        nodeStatusDto.nodeName = Constants.NODE_NAME;
        nodeStatusDto.nodeVersion = Constants.NODE_VERSION;

        List<StatusDto> status = new ArrayList<>();
        Collection<BlockChain> allBranch = branchGroup.getAllBranch();
        for (BlockChain bc : allBranch) {
            BranchId branchId = bc.getBranchId();
            long blockHeight = bc.getBlockChainManager().getLastIndex();
            long peerCount = peerTableGroup.getPeerTable(branchId).getPeerUriList().size();

            status.add(StatusDto.createBy(branchId.toString(), blockHeight, peerCount));
        }

        nodeStatusDto.status = status;
        nodeStatusDto.activePeerList = peerTableGroup.getActivePeerListWithStatus();
        return nodeStatusDto;
    }

}