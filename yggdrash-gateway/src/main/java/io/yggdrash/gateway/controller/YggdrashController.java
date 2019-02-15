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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("yggdrash/**")
class YggdrashController {

    private BranchId stemBranchId;

    @Autowired
    public YggdrashController(BranchGroup branchGroup) {
        this.stemBranchId = BranchId.NULL;
        // TODO change Stem Controller
        branchGroup.getAllBranch().forEach(branch -> {
            if (branch.getBranch().isYggdrash()) {
                // YGGDRASH Branch has Stem Contract
                this.stemBranchId = branch.getBranchId();
            }
        });
    }

    @GetMapping
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return "forward:" + "/branches/" + stemBranchId + uri.substring("/yggdrash".length()) + query;
    }
}
