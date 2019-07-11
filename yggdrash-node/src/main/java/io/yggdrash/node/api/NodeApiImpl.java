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

package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.gateway.dto.NodeStatusDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class NodeApiImpl implements NodeApi {

    private final BranchGroup branchGroup;
    private final PeerTableGroup peerTableGroup;

    @Autowired
    public NodeApiImpl(BranchGroup branchGroup, PeerTableGroup peerTableGroup) {
        this.branchGroup = branchGroup;
        this.peerTableGroup = peerTableGroup;
    }

    @Override
    public NodeStatusDto getNodeStatus() {
        return NodeStatusDto.createBy(branchGroup, peerTableGroup);
    }
}
