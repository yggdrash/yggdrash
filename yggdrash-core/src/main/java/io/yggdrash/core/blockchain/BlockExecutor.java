/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.BranchStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BlockExecutor {
    private final BlockStore store;
    private final BranchStore branchStore;
    private final Runtime runtime;
    private boolean runExecute;


    private static final Logger log = LoggerFactory.getLogger(BlockExecutor.class);

    BlockExecutor(BlockStore store, BranchStore branchStore, Runtime runtime) {
        this.store = store;
        this.branchStore = branchStore;
        this.runtime = runtime;
    }

    public boolean isRunExecute() {
        return runExecute;
    }

    void runExecuteBlocks() {
        if (!runExecute) {
            executeBlock();
        }
    }

    private void executeBlock() {
        // Run Block
        // GET BEST BLOCK
        long bestBlock = branchStore.getBestBlock();
        long lastExecuteBlock = branchStore.getLastExecuteBlockIndex();
        // TODO Validate Block will be stored
        if (bestBlock > lastExecuteBlock) {
            runExecute = true;
            while (lastExecuteBlock < bestBlock) {
                lastExecuteBlock++;
                BlockHusk block = store.getBlockByIndex(lastExecuteBlock);
                if (block == null) {
                    throw new FailedOperationException("Blockchain is not wired");
                }
                // Block Execute
                // TODO get block execute root state
                runtime.invokeBlock(block);
                // Set Next ExecuteBlock
                branchStore.setLastExecuteBlock(block);
                log.info("Block " + block.getIndex() + " Execute Complete");
            }
            runExecute = false;
        }
    }
}
