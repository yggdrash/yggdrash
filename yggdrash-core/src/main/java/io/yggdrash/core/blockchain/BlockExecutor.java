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

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import java.util.Map;

public class BlockExecutor implements Runnable {
    BlockStore store;
    MetaStore metaStore;
    Runtime runtime;

    public BlockExecutor(BlockStore store, Runtime runtime ) {
        this.store = store;
        this.runtime = runtime;
    }
    private Map<Sha3Hash, Boolean> executeBlock(BlockHusk block) {
        return runtime.invokeBlock(block);
    }

    public void executeBlocks() {
        // Run Block
        // GET BEST BLOCK
        long bestBlock = metaStore.getBestBlock();
        long lastExecuteBlock = metaStore.getLastExecuteBlockIndex();
        if (bestBlock > lastExecuteBlock) {
            while (lastExecuteBlock < bestBlock) {
                lastExecuteBlock++;
                BlockHusk block = store.getBlockByIndex(lastExecuteBlock);

                if (block == null) {
                    throw new FailedOperationException("Blockchain is not wired");
                }
                // Block Execute
                executeBlock(block);

                //


                // GET Next ExecuteBlock
                metaStore.setLastExecuteBlock(block);
            }
        }
        try {
            Thread.sleep(500);
            executeBlocks();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        executeBlocks();
    }
}
