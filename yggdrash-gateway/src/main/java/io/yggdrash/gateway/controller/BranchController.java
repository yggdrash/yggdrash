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

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.gateway.dto.BranchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("branches")
public class BranchController {
    private final BranchGroup branchGroup;

    @Autowired
    public BranchController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping
    public ResponseEntity<Map<String, BranchDto>> getBranches() {
        Map<String, BranchDto> branchMap = new HashMap<>();
        branchGroup.getAllBranch().forEach(blockChain ->
                branchMap.put(blockChain.getBranchId().toString(),
                        BranchDto.of(blockChain.getBranch().getJson())));
        return ResponseEntity.ok(branchMap);
    }

    @GetMapping("/{branchId}/states")
    public ResponseEntity<Long> getBranchStates(
            @PathVariable(name = "branchId") String branchId) {
        /*
            FIXME 브랜치 상태는 각 브랜치 별로 다르므로, 각 브랜치의 상태를 재정의하여 출력하는게 필요하다 해당 내용은 브랜치의
            상태값의 개수만 확인하는 것으로, 전반적인 변경이 필요하며, 블록체인상에서의 모든 상태정보를 출력하는 것은
            본질적으로 가능하지 않다. 노드에서의 영역이 아닌 서비스의 영역에서 해당 내용을 재정립하여, 확인 하여야 한다
         */
        Long stateSize = branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getStateSize();
        return ResponseEntity.ok(stateSize);
    }

}
