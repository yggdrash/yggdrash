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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.gateway.dto.NodeStatusDto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.yggdrash.node.api.JsonRpcConfig.NODE_API;
import static org.junit.Assert.assertNotNull;

public class NodeApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(NodeApiImplTest.class);

    private static BranchGroup branchGroup;
    private static PeerTableGroup peerTableGroup;
    private static NodeApiImpl nodeApi;

    @Before
    public void setUp() throws Exception {
        branchGroup = BlockChainTestUtils.createBranchGroup();
        peerTableGroup = PeerTestUtils.createTableGroup();
        nodeApi = new NodeApiImpl(branchGroup, peerTableGroup);
        ;
    }

    @Test
    public void logApiIsNotNull() {
        assertNotNull(NODE_API);
    }

    @Test
    public void getNodeStatusTest() {
        NodeStatusDto res = nodeApi.getNodeStatus();
        assertNotNull(res);

        printResult(res);
    }

    @Test
    public void getNodeStatusJsonRpcTest() {
        try {
            printResult(NODE_API.getNodeStatus());
        } catch (Exception e) {
            log.debug("getNodeStatusTest :: ERR => {}", e.getMessage());
        }
    }

    private void printResult(NodeStatusDto res) {
        log.debug("nodeName => {}", res.nodeName);
        log.debug("nodeVersion => {}", res.nodeVersion);
        log.debug("activePeerList => {}", res.activePeerList);
        log.debug("status size => {}", res.status.size());
        String statusFormat = "branchId=%s, blockHeight=%d, peerCount=%d";
        res.status.stream()
                .map(s -> String.format(statusFormat, s.branchId, s.blockHeight, s.peerCount)).forEach(log::debug);
    }
}