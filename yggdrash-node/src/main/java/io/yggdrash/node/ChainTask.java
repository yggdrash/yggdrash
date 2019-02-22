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

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class ChainTask {
    private static final Logger log = LoggerFactory.getLogger(ChainTask.class);

    @Autowired
    private NodeStatus nodeStatus;

    @Autowired
    private BranchGroup branchGroup;

    @Autowired
    private Wallet wallet;

    @Scheduled(fixedRate = 10000)
    public void generateBlock() {
        if (!nodeStatus.isUpStatus()) {
            log.debug("Waiting for up status...");
            return;
        }

        for (BranchId branchId : branchGroup.getAllBranchId()) {
            branchGroup.generateBlock(wallet, branchId);
        }
    }
}
