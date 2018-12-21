/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.google.gson.JsonArray;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

@RunWith(SpringRunner.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    /**
     * test generate block with large tx.
     */
    @Test(timeout = 5000L)
    public void generate100BlockTest() {
        BranchGroup branchGroup = new BranchGroup();
        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(blockChain, new BranchEventListener() {
            @Override
            public void chainedBlock(BlockHusk block) {
            }

            @Override
            public void receivedTransaction(TransactionHusk tx) {
            }
        });

        StopWatch watch = new StopWatch("generateBlockTest");

        watch.start("txStart");
        for (int i = 0; i < 100; i++) {
            TransactionHusk tx = createTx(i);
            blockChain.addTransaction(tx);
        }
        watch.stop();

        log.debug(watch.shortSummary());

        watch.start("addBlock");
        branchGroup.generateBlock(TestConstants.wallet(), blockChain.getBranchId());
        watch.stop();

        log.debug(watch.shortSummary());
    }

    private TransactionHusk createTx(int amount) {
        JsonArray txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount);
        return BlockChainTestUtils.createTxHusk(TestConstants.YEED, txBody);
    }
}