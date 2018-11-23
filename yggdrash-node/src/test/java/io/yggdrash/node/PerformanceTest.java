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

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchEventListener;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.TransactionHusk;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

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
        BlockChain blockChain = TestUtils.createBlockChain(false);
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
            blockChain.addTransaction(new TransactionHusk(TestUtils.sampleTransferTx(i)));
        }
        watch.stop();

        log.debug(watch.shortSummary());

        watch.start("addBlock");
        branchGroup.generateBlock(TestUtils.wallet(), blockChain.getBranchId());
        watch.stop();

        log.debug(watch.shortSummary());
    }

}