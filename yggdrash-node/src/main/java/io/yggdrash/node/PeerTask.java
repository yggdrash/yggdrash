/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerHandlerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class PeerTask extends io.yggdrash.core.net.PeerTask {

    @Autowired
    public void setPeerTable(KademliaPeerTable peerTable) {
        super.setPeerTable(peerTable);
    }

    @Autowired
    public void setPeerHandlerGroup(PeerHandlerGroup peerHandlerGroup) {
        super.setPeerHandlerGroup(peerHandlerGroup);
    }

    @Autowired
    public void setNodeStatus(NodeStatus nodeStatus) {
        super.setNodeStatus(nodeStatus);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void healthCheck() {
        super.healthCheck();
    }

    // refresh performs a lookup for a random target to keep buckets full.
    // seed nodes are inserted if the table is empty (initial bootstrap or discarded faulty peers).
    @Scheduled(cron = "*/30 * * * * *")
    public void refresh() {
        super.refresh();
    }

    // revalidate checks that the last node in a random bucket is still live
    // and replaces or deletes the node if it isn't
    @Scheduled(cron = "*/10 * * * * *")
    public void revalidate() {
        super.revalidate();
    }

    // copyNode adds peers from the table to the database if they have been in the table
    // longer then minTableTime.
    @Scheduled(cron = "*/30 * * * * *")
    public void copyNode() {
        super.copyNode();
    }
}
