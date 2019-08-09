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

package io.yggdrash.gateway.controller;

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@RestController
@RequestMapping("branches/{branchId}/logs")
public class LogController {

    private final BranchGroup branchGroup;

    @Autowired
    public LogController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping("/{index}")
    public ResponseEntity getLog(@PathVariable(name = BRANCH_ID) String branchId,
                                 @PathVariable(name = "index") long index) {
        return ResponseEntity.ok(branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getLog(index));
    }

    @GetMapping
    public ResponseEntity getLogs(@PathVariable(name = BRANCH_ID) String branchId,
                                  @RequestParam(name = "start") long start,
                                  @RequestParam(name = "offset") long offset) {
        return ResponseEntity.ok(branchGroup.getBranch(BranchId.of(branchId)).getContractManager()
                .getLogs(start, offset));
    }

    @GetMapping("/last")
    public ResponseEntity curIndex(@PathVariable(name = BRANCH_ID) String branchId) {
        return ResponseEntity.ok(branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getCurLogIndex());
    }
}