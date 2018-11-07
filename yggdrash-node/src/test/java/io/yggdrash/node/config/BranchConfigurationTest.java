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
import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    private static final String BRANCH_PATH
            = new DefaultConfig().getConfig().getString(Constants.BRANCH_PATH);
    private final Peer owner = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
    private final PeerGroup peerGroup = new PeerGroup(owner, 1);
    private BranchConfiguration branchConfiguration;
    private MockEnvironment mockEnv;

    @Before
    public void setUp() throws IOException {
        this.mockEnv = new MockEnvironment();
        branchConfiguration = new BranchConfiguration(mockEnv);

        JsonObject branch = getBranch();
        saveFile(branch);
    }

    @Test
    public void branchLoaderTest() {
        BranchGroup branchGroup = branchConfiguration.branchGroup();
        branchConfiguration.branchLoader(peerGroup, branchGroup, null, new DefaultConfig());
        assert branchGroup.getAllBranch().size() == 1;
    }

    @After
    public void tearDown() throws IOException {
        JsonObject branch = getBranch();
        BranchId branchId = BranchId.of(branch);
        File branchDir = new File(BRANCH_PATH, branchId.toString());
        FileUtils.deleteQuietly(branchDir);
    }

    private JsonObject getBranch() throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:/branch/metacoin.json");
        Reader json = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        return Utils.parseJsonObject(json);
    }

    private void saveFile(JsonObject branch) throws IOException {
        BranchId branchId = BranchId.of(branch);
        File branchDir = new File(BRANCH_PATH, branchId.toString());
        if (!branchDir.exists()) {
            branchDir.mkdirs();
        }
        File file = new File(branchDir, "branch.json");
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(branch);
        FileUtils.writeStringToFile(file, json);
    }

}