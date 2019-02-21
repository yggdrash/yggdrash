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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.ContractPolicyLoader;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.store.StoreBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class BranchConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(BranchConfigurationTest.class);

    private static final DefaultConfig config = new DefaultConfig();
    private static final ContractPolicyLoader policyLoader = new ContractPolicyLoader();
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private BranchConfiguration branchConfig;

    @Before
    public void setUp() {
        StoreBuilder builder = new StoreBuilder(config);
        this.branchConfig = new BranchConfiguration(builder);
    }

    @Test
    @Ignore
    public void branchLoaderTest() throws IOException {
        JsonObject branchJson = getBranchJson();
        BranchId branchId = BranchId.of(branchJson);

        saveFile(branchId, branchJson);
        BranchGroup branchGroup = branchConfig.branchGroup();
        branchConfig.branchLoader(config, branchGroup, policyLoader);
        File branchDir = new File(config.getBranchPath(), branchId.toString());
        FileUtils.deleteQuietly(branchDir);

        BlockChain branch = branchGroup.getBranch(branchId);
        assert branch != null;
        assert branch.getBranchId().equals(branchId);
        assertTransaction(branch);
    }

    private void assertTransaction(BlockChain branch) throws IOException {
        BlockHusk genesis = branch.getBlockByIndex(0);
        log.debug(genesis.toJsonObject().toString());
        TransactionHusk genesisTx = genesis.getBody().get(0);
        String txSignature = Hex.toHexString(genesisTx.getSignature());
        JsonObject branchJson = getBranchJson();
        assert txSignature.equals(branchJson.get("signature").getAsString());

        JsonArray jsonArrayTxBody = genesisTx.toJsonObject().getAsJsonArray("body");
        assert jsonArrayTxBody.size() == 1;
        JsonObject jsonObjectTxBody = jsonArrayTxBody.get(0).getAsJsonObject();
        assert jsonObjectTxBody.has("method");
        assert jsonObjectTxBody.has("branch");
        assert jsonObjectTxBody.has("params");
    }

    private JsonObject getBranchJson() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:/branch/branch-yggdrash.json");
        Reader json = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        return JsonUtil.parseJsonObject(json);
    }

    private void saveFile(BranchId branchId, JsonObject branch) throws IOException {
        File branchDir = new File(config.getBranchPath(), branchId.toString());
        if (!branchDir.exists() && branchDir.mkdirs()) {
            log.error("can't create at " + branchDir);
        }
        File file = new File(branchDir, BranchLoader.BRANCH_FILE);
        FileUtils.writeStringToFile(file, branch.toString(), StandardCharsets.UTF_8);
    }

}