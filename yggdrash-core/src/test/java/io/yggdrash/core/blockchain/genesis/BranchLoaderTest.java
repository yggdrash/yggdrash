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

package io.yggdrash.core.blockchain.genesis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.core.blockchain.Branch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BranchLoaderTest {
    private Branch branch;

    @Before
    public void setUpBranch() throws IOException {
        String genesisString = FileUtil.readFileToString(TestConstants.branchFile, FileUtil.DEFAULT_CHARSET);
        JsonObject branchJson = new JsonParser().parse(genesisString).getAsJsonObject();
        this.branch = Branch.of(branchJson);
    }

    @After
    public void deleteTestBranch() {
        String branchPath = new DefaultConfig().getBranchPath();
        Path targetBranchPath = Paths.get(branchPath, branch.getBranchId().toString());
        FileUtil.recursiveDelete(targetBranchPath);
    }

    @Test
    public void getBranchInfo() {
        String branchPath = new DefaultConfig().getBranchPath();
        BranchLoader loader = new BranchLoader(branchPath);
        Assert.assertNotNull(loader.getGenesisBlockList());
    }

    @Test
    public void saveBranchTest() throws IOException {
        String branchPath = new DefaultConfig().getBranchPath();
        BranchLoader loader = new BranchLoader(branchPath);
        Assert.assertTrue(loader.saveBranch(branch));
        // reload branch by branch.json
        Path targetBranch = Paths.get(branchPath, branch.getBranchId().toString(), BranchLoader.BRANCH_FILE);
        String reloadBranch = FileUtil.readFileToString(targetBranch.toFile(), FileUtil.DEFAULT_CHARSET);
        JsonObject branchJson = new JsonParser().parse(reloadBranch).getAsJsonObject();

        Assert.assertEquals(Branch.of(branchJson).getBranchId(), branch.getBranchId());

    }
}