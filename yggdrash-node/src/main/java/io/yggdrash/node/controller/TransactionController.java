/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.controller;

import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("txs")
public class TransactionController {

    private final BranchGroup branchGroup;

    @Autowired
    public TransactionController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @PostMapping
    public ResponseEntity add(@RequestBody TransactionDto request) {
        TransactionHusk tx = TransactionDto.of(request);
        TransactionHusk addedTx = branchGroup.addTransaction(tx);
        return ResponseEntity.ok(TransactionDto.createBy(addedTx));
    }

    @GetMapping("{branchId}/{id}")
    public ResponseEntity get(@PathVariable(name = "branchId") String branchId,
                              @PathVariable String id) {
        TransactionHusk tx = branchGroup.getTxByHash(BranchId.of(branchId), id);

        if (tx == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(TransactionDto.createBy(tx));
    }

    @GetMapping("{branchId}")
    public ResponseEntity getAll(@PathVariable(name = "branchId") String branchId) {
        List<TransactionHusk> txs = branchGroup.getTransactionList(BranchId.of(branchId));
        List<TransactionDto> dtoList = txs.stream().sorted(Comparator.reverseOrder())
                .map(TransactionDto::createBy).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
