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
import io.yggdrash.core.exception.NotValidateException;
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

    private final Queue<String> nodeQueue = new LinkedBlockingQueue<>();

    private final MessageSender messageSender;

    private final NodeManager nodeManager;

    @Autowired
    public NodeScheduler(MessageSender messageSender, NodeManager nodeManager) {
        this.messageSender = messageSender;
        this.nodeManager = nodeManager;
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void ping() {
        messageSender.ping();
    }

    @Scheduled(initialDelay = 1000 * 5, fixedRate = 1000 * BLOCK_MINE_SEC)
    public void generateBlock() throws IOException, NotValidateException {
        if (nodeQueue.isEmpty()) {
            nodeQueue.addAll(nodeManager.getPeerUriList());
        }
        String peerId = nodeQueue.poll();
        if (peerId != null && peerId.equals(nodeManager.getNodeUri())) {
            nodeManager.generateBlock();
        } else {
            assert peerId != null;
            log.debug("ignored peer=" + peerId.substring(peerId.lastIndexOf(":")));
        }
    }

}
