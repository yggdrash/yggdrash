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
import io.yggdrash.core.blockchain.BranchGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static io.yggdrash.node.api.JsonRpcConfig.LOG_API;
import static org.junit.Assert.assertNotNull;

public class LogApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(LogApiImplTest.class);
    private static LogApiImpl logApi;
    private static BranchGroup branchGroup;
    private String branchId;

    @Before
    public void setUp() throws Exception {
        branchGroup = BlockChainTestUtils.createBranchGroup();
        logApi = new LogApiImpl(branchGroup);
        branchId = BlockChainTestUtils.createTransferTx().getBranchId().toString();
    }

    @Test
    public void logApiIsNotNull() {
        assertNotNull(LOG_API);
    }

    @Test
    public void getLogJsonRpcTest() {
        try {
            String res = LOG_API.getLog(branchId, 0);
        } catch (Exception e) {
            log.debug("getLogTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void getLogsJsonRpcTest() {
        try {
            //LOG_API.getLogs(branchId, 0, 9);
            LOG_API.getLogs(branchId, "\\W*(Total)\\W*", 0, 100);
        } catch (Exception e) {
            log.debug("getLogsTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void curIndexJsonRpcTest() {
        try {
            long res = LOG_API.curIndex(branchId);
        } catch (Exception e) {
            log.debug("curIndexTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void getLogsByRegexTest() {
        List<String> res = logApi.getLogs(branchId, "\\W*(Total)\\W*", 0, 100);
        Assert.assertEquals(1, res.size());
        Assert.assertTrue(res.contains("Total Supply is 1994000000000000000000000"));

        res = logApi.getLogs(branchId, "\\W*(TTotal)\\W*", 0, 100);
        Assert.assertEquals(0, res.size());
    }

    @Test
    public void regexTest() {
        String log = "Propose 6a95d72869643550a485cbea0a4a8aac1092b4b185f58903b1d348383068ca42 ISSUED";
        Assert.assertTrue(Pattern.compile("^(Propose [a-f0-9]{64} ISSUED)").matcher(log).find());
        Assert.assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} PROCESSING)").matcher(log).find());
        Assert.assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} DONE)").matcher(log).find());
        Assert.assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} CLOSED)").matcher(log).find());

        String yeedLog = "Total Supply is 1994000000000000000000000";
        Assert.assertTrue(Pattern.compile("\\W*(Total)\\W*").matcher(yeedLog).find());
    }

}