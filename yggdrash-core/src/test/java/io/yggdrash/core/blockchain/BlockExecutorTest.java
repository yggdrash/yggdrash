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
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Assert;
import org.junit.Test;

public class BlockExecutorTest {

    private static final BranchId BRANCH_ID = BranchId.NULL;
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    @Test
    public void executorTest() {

        Runtime runtime = new Runtime(
                        new StateStore(new HashMapDbSource()),
                        new TransactionReceiptStore(new HashMapDbSource())
                );
        runtime.addContract(ContractVersion.of("c10e873655becf550c4aece75a091f4553d6202d"), stemContract);

        // Block Store
        // Blockchain Runtime

        StoreBuilder builder = StoreBuilder.newBuilder()
                .setConfig(new DefaultConfig())
                .setBranchId(BRANCH_ID)
                .setBlockStoreFactory(PbftBlockStoreMock::new);

        ConsensusBlockStore store = builder.buildBlockStore();
        BranchStore branchStore = builder.buildBranchStore();

        BlockExecutor ex = new BlockExecutor(store, branchStore, runtime);

        // BlockStore add genesis block and other

        ex.runExecuteBlocks();
        Assert.assertFalse(ex.isRunExecute());
    }
}
