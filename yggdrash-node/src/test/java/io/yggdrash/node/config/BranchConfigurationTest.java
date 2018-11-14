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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class BranchConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(BranchConfigurationTest.class);

    private static final String BRANCH_PATH = new DefaultConfig().getBranchPath();
    private final Peer owner = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
    private final PeerGroup peerGroup = new PeerGroup(owner, 1);
    private BranchConfiguration branchConfiguration;
    private MockEnvironment mockEnv;
    private BranchId branchId;

    @Before
    public void setUp() throws IOException {
        this.mockEnv = new MockEnvironment();
        this.branchConfiguration = new BranchConfiguration(mockEnv);

        JsonObject branchJson = getBranchJson();
        this.branchId = BranchId.of(branchJson);

        saveFile(branchJson);
    }

    @Test
    public void branchLoaderTest() throws IOException {
        BranchGroup branchGroup = branchConfiguration.branchGroup();
        branchConfiguration.branchLoader(peerGroup, branchGroup, null, new DefaultConfig());
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

        JsonArray jsonArrayTxBody = genesisTx.toJsonObject().get("body").getAsJsonArray();
        assert jsonArrayTxBody.size() == 1;
        JsonObject jsonObjectTxBody = jsonArrayTxBody.get(0).getAsJsonObject();
        assert jsonObjectTxBody.has("method");
        assert jsonObjectTxBody.has("branch");
        assert jsonObjectTxBody.has("params");
    }

    @After
    public void tearDown() {
        File branchDir = new File(BRANCH_PATH, branchId.toString());
        FileUtils.deleteQuietly(branchDir);
    }

    private JsonObject getBranchJson() throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:/branch/metacoin.json");
        Reader json = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        return Utils.parseJsonObject(json);
    }

    private void saveFile(JsonObject branch) throws IOException {
        File branchDir = new File(BRANCH_PATH, branchId.toString());
        if (!branchDir.exists()) {
            branchDir.mkdirs();
        }
        File file = new File(branchDir, "branch.json");
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(branch);
        FileUtils.writeStringToFile(file, json);
    }

}