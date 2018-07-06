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

import io.yggdrash.core.Block;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.exception.NotValidteException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("blocks")
class BlockController {

    private final NodeManager nodeManager;

    public BlockController(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @PostMapping
    public ResponseEntity add() throws IOException, NotValidteException {
        Block generatedBlock = nodeManager.generateBlock();
        return ResponseEntity.ok(BlockDto.createBy(generatedBlock));
    }

    @GetMapping("{id}")
    public ResponseEntity get(@PathVariable(name = "id") String id) throws IOException {
        Block foundBlock = nodeManager.getBlockByIndexOrHash(id);

        if (foundBlock == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(BlockDto.createBy(foundBlock));
        }
    }

    @GetMapping
    public ResponseEntity getAll() {
        Set<Block> blocks = nodeManager.getBlocks();
        return ResponseEntity.ok(blocks);
    }
}