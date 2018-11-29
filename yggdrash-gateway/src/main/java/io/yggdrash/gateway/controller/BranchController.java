/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.gateway.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.node.api.dto.BranchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("branches")
public class BranchController {
    private static final Logger log = LoggerFactory.getLogger(BranchController.class);

    private final BranchGroup branchGroup;

    @Autowired
    public BranchController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping
    public ResponseEntity<Map<String, BranchDto>> getBranches() {
        Map<String, BranchDto> branchMap = new HashMap<>();
        for (BlockChain branch : branchGroup.getAllBranch()) {
            BlockHusk genesis = branch.getBlockByIndex(0);
            BranchDto branchJson = getBranchJson(genesis);
            branchMap.put(branch.getBranchId().toString(), branchJson);
        }
        return ResponseEntity.ok(branchMap);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Long>> getAll() {
        Map<String, Long> activeMap = new HashMap<>();
        branchGroup.getAllBranch().forEach(branch ->
                activeMap.put(branch.getBranchId().toString(), branch.getLastIndex()));
        return ResponseEntity.ok(activeMap);
    }

    @GetMapping("/{branchId}/states")
    public ResponseEntity<List> getStates(@PathVariable(name = "branchId") String branchId) {
        List state = branchGroup.getStateStore(BranchId.of(branchId)).getStateList();
        return ResponseEntity.ok(state);
    }

    private BranchDto getBranchJson(BlockHusk genesis) {
        for (TransactionHusk tx : genesis.getBody()) {
            JsonArray txBody = tx.toJsonObject().getAsJsonArray("body");
            if (txBody.size() != 0) {
                return getBranchJson(txBody);
            }
        }
        return new BranchDto();
    }

    private BranchDto getBranchJson(JsonArray txBody) {
        try {
            JsonElement firstTx = txBody.get(0);
            if (!firstTx.isJsonObject()) {
                log.warn("Genesis tx is not jsonObject.");
            } else if (!firstTx.getAsJsonObject().has("branch")) {
                log.warn("Genesis tx does not contains branch property.");
            } else {
                JsonObject branchJson = firstTx.getAsJsonObject().get("branch").getAsJsonObject();
                JsonElement genesis
                        = firstTx.getAsJsonObject().get("params").getAsJsonArray().get(0);
                branchJson.add("genesis", genesis);
                return BranchDto.of(branchJson);
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
        return new BranchDto();
    }
}
