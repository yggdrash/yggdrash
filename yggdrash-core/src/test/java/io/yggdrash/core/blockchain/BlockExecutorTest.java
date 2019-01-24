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

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.contract.CoinContract;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Test;

public class BlockExecutorTest {

    private static final BranchId BRANCH_ID = BranchId.NULL;

    @Test
    public void executorTest() {

        CoinContract contract = new CoinContract();
        Runtime runtime =
                new Runtime<>(
                        new StateStore<>(new HashMapDbSource()),
                        new TransactionReceiptStore(new HashMapDbSource())
                );
        runtime.addContract(ContractId.of("coinContract"), contract);

        // Block Store
        // Blockchain Runtime
        StoreBuilder builder = new StoreBuilder(new DefaultConfig(false));
        BlockStore store = builder.buildBlockStore(BRANCH_ID);
        MetaStore meta = builder.buildMetaStore(BRANCH_ID);

        BlockExecutor ex = new BlockExecutor(store, meta, runtime);

        // BlockStore add genesis block and other

        ex.runExecuteBlocks();

    }
}
