/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node.api;

import io.yggdrash.BlockChainTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.yggdrash.node.api.JsonRpcConfig.LOG_API;
import static org.junit.Assert.assertNotNull;

public class LogApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(LogApiImplTest.class);
    private String branchId;

    @Before
    public void setUp() throws Exception {
        branchId = BlockChainTestUtils.createTransferTx().getBranchId().toString();
    }

    @Test
    public void logApiIsNotNull() {
        assertNotNull(LOG_API);
    }

    @Test
    public void getLogTest() {
        try {
            String res = LOG_API.getLog(branchId, 0);
        } catch (Exception e) {
            log.debug("getLogTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void getLogsTest() {
        try {
            List<String> res = LOG_API.getLogs(branchId, 0, 9);
        } catch (Exception e) {
            log.debug("getLogsTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void curIndexTest() {
        try {
            long res = LOG_API.curIndex(branchId);
        } catch (Exception e) {
            log.debug("curIndexTest :: ERR => {}", e.getMessage());
        }
    }
}