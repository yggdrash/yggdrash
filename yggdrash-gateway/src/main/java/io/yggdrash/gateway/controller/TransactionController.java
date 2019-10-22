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

package io.yggdrash.gateway.controller;

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.gateway.dto.TransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@RestController
@RequestMapping("branches/{branchId}/txs")
class TransactionController {

    private final BranchGroup branchGroup;

    @Autowired
    public TransactionController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @PostMapping
    public ResponseEntity add(@PathVariable(name = BRANCH_ID) String branchId,
                              @RequestBody TransactionDto request) {
        Transaction tx = TransactionDto.of(request);
        if (BranchId.of(branchId).equals(tx.getBranchId())) {
            if (branchGroup.addTransaction(tx).size() == 0) {
                return ResponseEntity.ok(TransactionDto.createBy(tx));
            } else {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable(name = BRANCH_ID) String branchId,
                              @PathVariable String id) {
        Transaction tx = branchGroup.getTxByHash(BranchId.of(branchId), id);

        return ResponseEntity.ok(TransactionDto.createBy(tx));
    }

    @GetMapping
    public ResponseEntity getAll(@PathVariable(name = BRANCH_ID) String branchId) {
        List<Transaction> txs = new ArrayList<>(branchGroup.getRecentTxs(BranchId.of(branchId)));
        List<TransactionDto> dtoList = txs.stream().sorted(Comparator.reverseOrder())
                .map(TransactionDto::createBy).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("txs", dtoList);
        return ResponseEntity.ok(res);
    }
}
