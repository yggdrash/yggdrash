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

import com.google.gson.JsonObject;
import io.yggdrash.core.BranchGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity getAll() {
        Map<String, ?> state = branchGroup.getStateStore().getState();
        Map<Object, Object> result = new HashMap<>();
        for (Map.Entry entry : state.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof JsonObject) {
                result.put(entry.getKey(), value.toString());
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return ResponseEntity.ok(result);
    }
}
