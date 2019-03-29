/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.SyncManager;
import io.yggdrash.core.blockchain.TransactionKvIndexer;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.core.net.BootStrapNode;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.node.config.NodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class YggdrashNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(YggdrashNodeApp.class);

    private final NodeProperties nodeProperties;

    @Autowired
    @Override
    public void setSyncManager(SyncManager syncManager) {
        super.setSyncManager(syncManager);
    }

    @Autowired
    @Override
    public void setNodeStatus(NodeStatus nodeStatus) {
        super.setNodeStatus(nodeStatus);
    }

    @Autowired
    @Override
    public void setPeerNetwork(PeerNetwork peerNetwork) {
        super.setPeerNetwork(peerNetwork);
    }

    @Autowired
    @Override
    public void setBranchGroup(BranchGroup branchGroup) {
        super.setBranchGroup(branchGroup);
    }

    @Autowired
    public void setTransactionKvIndexer(TransactionKvIndexer txKvIndexer) {
        super.setTxKvIndexer(txKvIndexer);
    }

    @Autowired
    public void setListener(BlockChainConsumer blockChainConsumer) {
        blockChainConsumer.setListener((BlockChainSyncManager) syncManager);
    }

    @Autowired
    YggdrashNode(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @PostConstruct
    public void init() {
        if (nodeProperties.isValidator()) {
            log.info("I'm the Validator Node.");
            return;
        }

        log.info("Bootstrapping...");
        bootstrapping();

        if (nodeProperties.isSeed()) {
            log.info("I'm the Bootstrap Node.");
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Terminating...");
        peerNetwork.destroy();
    }
}
