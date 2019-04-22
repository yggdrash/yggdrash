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

import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public class ChainTask {
    private static final Logger log = LoggerFactory.getLogger(ChainTask.class);

    @Autowired
    private NodeStatus nodeStatus;

    @Autowired
    private BranchGroup branchGroup;

    @Autowired
    private Wallet wallet;

    @Scheduled(cron = "*/10 * * * * *")
    public void generateBlock() {
        if (!nodeStatus.isUpStatus()) {
            log.debug("Waiting for up status...");
            return;
        }

        for (BlockChain branch : branchGroup.getAllBranch()) {
            List<TransactionHusk> txs =
                    branch.getTransactionStore().getUnconfirmedTxsWithLimit(Constants.LIMIT.BLOCK_SYNC_SIZE);
            Block block = BlockHusk.nextBlock(wallet, txs, branch.getLastConfirmedBlock());
            branch.addBlock(block);
        }
    }
}
