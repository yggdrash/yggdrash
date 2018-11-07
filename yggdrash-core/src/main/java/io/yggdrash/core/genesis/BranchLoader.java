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

package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.account.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BranchLoader {
    private static final Logger log = LoggerFactory.getLogger(BranchLoader.class);

    private static final String BRANCH_FILE = "branch.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final File branchRoot;
    private final List<GenesisBlock> genesisBlockList = new ArrayList<>();

    public BranchLoader(DefaultConfig defaultConfig, Wallet wallet) {
        String branchPath = defaultConfig.getConfig().getString(Constants.BRANCH_PATH);
        this.branchRoot = new File(branchPath);
        if (!branchRoot.exists()) {
            branchRoot.mkdirs();
        }
        load(wallet);
    }

    private void load(Wallet wallet) {
        for (File branchDir : branchRoot.listFiles()) {
            File branchFile = new File(branchDir, BRANCH_FILE);
            try {
                BranchJson branchJson = MAPPER.readValue(branchFile, BranchJson.class);
                branchJson.branchId = branchDir.getName();
                genesisBlockList.add(new GenesisBlock(branchJson, wallet));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public List<GenesisBlock> getGenesisBlockList() {
        return genesisBlockList;
    }
}
