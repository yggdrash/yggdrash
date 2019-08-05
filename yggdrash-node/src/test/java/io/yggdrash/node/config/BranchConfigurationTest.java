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

package io.yggdrash.node.config;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore
public class BranchConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(BranchConfigurationTest.class);

    private static final DefaultConfig config = new DefaultConfig();
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private BranchConfiguration branchConfig;

    @Before
    public void setUp() {
        this.branchConfig = new BranchConfiguration(config);
    }

    @Test
    public void branchLoaderTest() throws IOException {
        JsonObject branchJson = getBranchJson();
        Branch branch = getBranchByJsonObj(branchJson);
        BranchId branchId = branch.getBranchId();
        log.debug("branchId : {}", branchId);

        saveFile(branchId, branchJson);
        BranchGroup branchGroup = branchConfig.branchGroup();
        branchConfig.branchLoader(config, branchGroup, new MockEnvironment());
        File branchDir = new File(config.getBranchPath(), branchId.toString());
        FileUtils.deleteQuietly(branchDir);

        BlockChain blockChain = branchGroup.getBranch(branchId);
        assertNotNull(blockChain);
        assertEquals(blockChain.getBranchId(), branchId);
        assertTransaction(blockChain);
    }

    private void assertTransaction(BlockChain branch) throws IOException {
        ConsensusBlock genesis = branch.getBlockChainManager().getBlockByIndex(0);
        log.debug(genesis.toJsonObject().toString());
        Transaction genesisTx = genesis.getBody().getTransactionList().get(0);

        JsonObject jsonTxBody = genesisTx.toJsonObject().getAsJsonObject("body");
        assertTrue(jsonTxBody.has("method"));
        assertTrue(jsonTxBody.has("contractVersion"));
        assertTrue(jsonTxBody.has("params"));
        assertTrue(jsonTxBody.has("isSystem"));
        assertTrue(jsonTxBody.has("consensus"));
    }

    private Branch getBranchByJsonObj(JsonObject branchJson) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:/branch-yggdrash.json");
        Reader json = new InputStreamReader(resource.getInputStream(), FileUtil.DEFAULT_CHARSET);
        return Branch.of(branchJson);
    }

    private JsonObject getBranchJson() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:/branch-yggdrash.json");
        Reader json = new InputStreamReader(resource.getInputStream(), FileUtil.DEFAULT_CHARSET);
        return JsonUtil.parseJsonObject(json);
    }

    private void saveFile(BranchId branchId, JsonObject branch) throws IOException {
        File branchDir = new File(config.getBranchPath(), branchId.toString());
        if (!branchDir.exists() && branchDir.mkdirs()) {
            log.error("can't create at " + branchDir);
        }
        File file = new File(branchDir, BranchLoader.BRANCH_FILE);
        FileUtils.writeStringToFile(file, branch.toString(), FileUtil.DEFAULT_CHARSET);
    }

}
