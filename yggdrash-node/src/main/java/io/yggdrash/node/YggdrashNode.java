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

import io.yggdrash.core.akashic.SyncManager;
import io.yggdrash.core.net.BootStrapNode;
import io.yggdrash.core.net.Discovery;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerHandlerGroup;
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

    private final NodeStatus nodeStatus;

    private final Discovery discovery;

    @Autowired
    public void setPeerHandlerGroup(PeerHandlerGroup peerHandlerGroup) {
        super.setPeerHandlerGroup(peerHandlerGroup);
    }

    @Autowired
    public void setSyncManager(SyncManager syncManager) {
        super.setSyncManager(syncManager);
    }

    YggdrashNode(NodeProperties nodeProperties, NodeStatus nodeStatus, Discovery discovery) {
        this.nodeProperties = nodeProperties;
        this.nodeStatus = nodeStatus;
        this.discovery = discovery;
    }

    @PostConstruct
    public void init() {
        log.info("Bootstrapping...");
        super.bootstrapping(discovery, nodeProperties.getMaxPeers());

        if (nodeProperties.isSeed()) {
            log.info("I'm the Bootstrap Node.");
            nodeStatus.up();
            return;
        }
        nodeStatus.sync();
        syncManager.syncBlockAndTransaction();
        nodeStatus.up();
    }

    @PreDestroy
    public void destroy() {
        log.info("Destroy handlerGroup");
        super.destroy();
    }
}
