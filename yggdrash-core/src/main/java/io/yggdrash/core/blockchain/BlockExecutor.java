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

import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;

public class BlockExecutor implements Runnable {
    BlockStore store;
    MetaStore metaStore;
    Runtime runtime;

    public BlockExecutor(BlockStore store, Runtime runtime ) {

    }

    public void executeBlock() {
        // Run Block
        // GET BEST BLOCK
        long bestBlock = metaStore.getBestBlock();
        long lastExecuteBlock = metaStore.getLastExecuteBlockIndex();
        if (bestBlock > lastExecuteBlock) {
            while (lastExecuteBlock == bestBlock) {
                // GET Next ExecuteBlock
            }



        }
        //runtime.invokeBlock()
    }


    @Override
    public void run() {

    }
}
