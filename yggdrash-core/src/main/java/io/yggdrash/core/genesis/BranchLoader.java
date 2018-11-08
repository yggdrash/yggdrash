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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BranchLoader {
    private static final Logger log = LoggerFactory.getLogger(BranchLoader.class);

    private static final String BRANCH_FILE = "branch.json";

    private final File branchRoot;
    private final List<GenesisBlock> genesisBlockList = new ArrayList<>();

    public BranchLoader(String branchPath) {
        this.branchRoot = new File(branchPath);
        if (!branchRoot.exists()) {
            branchRoot.mkdirs();
        }
        load();
    }

    private void load() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (File branchDir : branchRoot.listFiles()) {
            File branchFile = new File(branchDir, BRANCH_FILE);
            try {
                BranchJson branchJson = objectMapper.readValue(branchFile, BranchJson.class);
                branchJson.branchId = branchDir.getName();
                genesisBlockList.add(new GenesisBlock(branchJson));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public List<GenesisBlock> getGenesisBlockList() {
        return genesisBlockList;
    }
}
