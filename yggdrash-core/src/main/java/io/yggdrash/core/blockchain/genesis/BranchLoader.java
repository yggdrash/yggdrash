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

import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BranchLoader {
    public static final String BRANCH_FILE = "branch.json";
    private static final Logger log = LoggerFactory.getLogger(BranchLoader.class);

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
        for (File branchDir : branchRoot.listFiles()) {
            File branchFile = new File(branchDir, BRANCH_FILE);
            if (!branchFile.exists()) {
                continue;
            }
            try (FileInputStream is = new FileInputStream(branchFile)) {
                GenesisBlock genesisBlock = GenesisBlock.of(is);
                genesisBlockList.add(genesisBlock);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    boolean saveBranch(Branch branch) {
        // Make branch Dir
        File branchDir = loadBranchDirectory(branch.getBranchId());
        if (!branchDir.exists()) { // IF not exist
            branchDir.mkdir();
            File branchFile = new File(branchDir, BRANCH_FILE);
            try (FileOutputStream outputStream = new FileOutputStream(branchFile)) {
                // Copy Branch File
                byte[] branchJson = branch.getJson().toString().getBytes();
                outputStream.write(branchJson);
                return true;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }

    public Optional<GenesisBlock> getGenesisBlock(BranchId branchId) {
        return genesisBlockList.stream()
                .filter(genesisBlock -> genesisBlock.getBranchId().equals(branchId))
                .findFirst();
    }

    public List<GenesisBlock> getGenesisBlockList() {
        return genesisBlockList;
    }

    private File loadBranchDirectory(BranchId branchId) {
        return new File(branchRoot, branchId.toString());
    }

}
