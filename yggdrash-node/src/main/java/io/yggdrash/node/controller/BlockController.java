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

package io.yggdrash.node.controller;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("blocks")
class BlockController {

    private final Wallet wallet;
    private final BranchGroup branchGroup;

    @Autowired
    public BlockController(Wallet wallet, BranchGroup branchGroup) {
        this.wallet = wallet;
        this.branchGroup = branchGroup;
    }

    @PostMapping
    public ResponseEntity add() {
        BlockHusk generatedBlock = branchGroup.generateBlock(wallet);
        return ResponseEntity.ok(BlockDto.createBy(generatedBlock));
    }

    @GetMapping("{id}")
    public ResponseEntity get(@PathVariable(name = "id") String id) {
        BlockHusk foundBlock = branchGroup.getBlockByIndexOrHash(id);

        if (foundBlock == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(BlockDto.createBy(foundBlock));
        }
    }

    @GetMapping
    public ResponseEntity getAll(@RequestParam(value = "offset", defaultValue = "25") long offset,
                                 @RequestParam(value = "limit", defaultValue = "25") int limit) {
        List<BlockDto> blocks = new ArrayList<>();
        long lastIdx = branchGroup.getLastIndex();
        if (offset > lastIdx) {
            offset = lastIdx;
        }
        for (int i = 0; i < limit && offset >= 0; i++) {
            BlockHusk block = branchGroup.getBlockByIndexOrHash(String.valueOf(offset--));
            if (block == null) {
                break;
            }
            blocks.add(BlockDto.createBy(block));
        }
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("latest")
    public ResponseEntity latest() {
        String latest = String.valueOf(branchGroup.getLastIndex());
        return ResponseEntity.ok(BlockDto.createBy(branchGroup.getBlockByIndexOrHash(latest)));
    }
}