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

package io.yggdrash.node;

import io.yggdrash.core.NodeManager;
import io.yggdrash.core.exception.NotValidteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
class NodeScheduler {
    private static final Logger log = LoggerFactory.getLogger(NodeScheduler.class);

    private static final int BLOCK_MINE_SEC = 10;

    private Queue<String> nodeQueue = new LinkedBlockingQueue<>();

    @Autowired
    MessageSender messageSender;

    @Autowired
    NodeManager nodeManager;

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void ping() {
        messageSender.ping();
    }

    //@Scheduled(cron = "*/" + BLOCK_MINE_SEC + " * * * * *")
    @Scheduled(initialDelay = 1000 * 5, fixedRate = 1000 * BLOCK_MINE_SEC)
    public void generateBlock() throws IOException, NotValidteException {
        if (nodeQueue.isEmpty()) {
            nodeQueue.addAll(messageSender.getPeerIdList());
        }
        String peerId = nodeQueue.poll();
        if (peerId != null && peerId.equals(nodeManager.getNodeId())) {
            nodeManager.generateBlock();
        } else {
            log.debug("ignored peerId=" + peerId);
        }
    }

}
