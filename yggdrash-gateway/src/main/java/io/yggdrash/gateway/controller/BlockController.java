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

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.node.api.dto.BlockDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("branches/{branchId}/blocks")
class BlockController {

    private final BranchGroup branchGroup;

    @Autowired
    public BlockController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable(name = "branchId") String branchId,
                              @PathVariable(name = "id") String id) {
        BlockHusk foundBlock;
        if (StringUtils.isNumeric(id)) {
            foundBlock = branchGroup.getBlockByIndex(BranchId.of(branchId), Long.valueOf(id));
        } else {
            foundBlock = branchGroup.getBlockByHash(BranchId.of(branchId), id);
        }

        if (foundBlock == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(BlockDto.createBy(foundBlock));
        }
    }

    @GetMapping
    public ResponseEntity getAll(@PathVariable(name = "branchId") String branchId,
                                 @RequestParam(value = "offset", required = false) Long offset,
                                 @RequestParam(value = "limit", defaultValue = "25") int limit) {
        List<BlockDto> blocks = new ArrayList<>();
        BranchId id = BranchId.of(branchId);
        long lastIdx = branchGroup.getLastIndex(id);

        if (offset == null) {
            offset = lastIdx;
        }

        for (int i = 0; i < limit && offset >= 0; i++) {
            BlockHusk block = branchGroup.getBlockByIndex(id, offset--);
            if (block == null) {
                break;
            }
            blocks.add(BlockDto.createBy(block));
        }
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("/latest")
    public ResponseEntity latest(@PathVariable(name = "branchId") String branchId) {
        BranchId id = BranchId.of(branchId);
        long latest = branchGroup.getLastIndex(id);
        return ResponseEntity.ok(BlockDto.createBy(branchGroup.getBlockByIndex(id, latest)));
    }

}
