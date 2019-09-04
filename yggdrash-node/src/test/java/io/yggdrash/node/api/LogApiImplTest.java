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
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.PbftProto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static io.yggdrash.node.api.JsonRpcConfig.LOG_API;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
            LOG_API.getLog(branchId, 0);
        } catch (Exception e) {
            log.debug("getLogTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void getLogsJsonRpcTest() {
        try {
            LOG_API.getLogs(branchId, 0, 9);
        } catch (Exception e) {
            log.debug("getLogsTest :: ERR => {}", e.getMessage());
        }
    }

    @Test
    public void getLogsByRegexJsonRpcTest() {
        try {
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
    public void getLogsOverallTest() {
        BlockChain bc = branchGroup.getBranch(BranchId.of(branchId));
        BlockChainManager mgr = bc.getBlockChainManager();
        int contractSize = bc.getBranchContracts().size();

        assertEquals("Last Index", 0, mgr.getLastIndex());
        assertEquals("Tx Count", contractSize, mgr.countOfTxs());

        int defaultLogIndex = contractSize + 5;
        assertEquals("Current Index", defaultLogIndex, logApi.curIndex(branchId));

        int generateTx = 33;
        ConsensusBlock<PbftProto.PbftBlock> block = BlockChainTestUtils.createBlockListFilledWithTx(1, generateTx).get(0);
        bc.addBlock(block);

        defaultLogIndex += generateTx;

        assertEquals("Last Index", 1, mgr.getLastIndex());
        assertEquals("Tx Count", generateTx + contractSize, mgr.countOfTxs());
        assertEquals("Current Index", defaultLogIndex, logApi.curIndex(branchId));


        Log log = logApi.getLog(branchId, 33);
        List<Log> logs = logApi.getLogs(branchId, 33, 33);

        assertEquals("Size of logs", 1, logs.size());
        assertEquals(log.getMsg(), logs.get(0).getMsg());

        long curIndex = logApi.curIndex(branchId);
        log = logApi.getLog(branchId, curIndex);
        logs = logApi.getLogs(branchId, curIndex, curIndex);

        assertEquals(1, logs.size());
        assertEquals(log.getMsg(), logs.get(0).getMsg());

        // Logs of executed transactions when the block was added.
        List<Log> regLogs = logApi.getLogs(branchId, "Transfe", 0, curIndex);
        assertEquals(33, regLogs.size());

        log = logApi.getLog(branchId, 0);
        logs = logApi.getLogs(branchId, 0, 0);
        assertEquals(log.getMsg(), logs.get(0).getMsg());

        logs = logApi.getLogs(branchId, -1, 0);
        assertEquals(log.getMsg(), logs.get(0).getMsg());

        log = logApi.getLog(branchId, -1);
        assertEquals("Log not exists", log.getMsg());
    }

    @Test
    public void getLogsByRegexTest() {
        List<Log> res = logApi.getLogs(branchId, "\\W*(Total)\\W*", 0, 100);
        assertEquals(1, res.size());
        res.stream().map(l -> l.getMsg().contains("Total Supply is 1994000000000000000000000"))
                .forEach(Assert::assertTrue);

        res = logApi.getLogs(branchId, "\\W*(TTotal)\\W*", 0, 100);
        assertEquals(0, res.size());
    }

    @Test
    public void regexTest() {
        String log = "Propose 6a95d72869643550a485cbea0a4a8aac1092b4b185f58903b1d348383068ca42 ISSUED";
        assertTrue(Pattern.compile("^(Propose [a-f0-9]{64} ISSUED)").matcher(log).find());
        assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} PROCESSING)").matcher(log).find());
        assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} DONE)").matcher(log).find());
        assertFalse(Pattern.compile("^(Propose [a-f0-9]{64} CLOSED)").matcher(log).find());

        String yeedLog = "Total Supply is 1994000000000000000000000";
        assertTrue(Pattern.compile("\\W*(Total)\\W*").matcher(yeedLog).find());
    }

}