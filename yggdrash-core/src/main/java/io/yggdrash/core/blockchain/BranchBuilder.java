/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.BranchContract;

import java.util.ArrayList;
import java.util.List;

public class BranchBuilder {
    private String name;
    private String symbol;
    private String property;
    private String description;
    private String timeStamp;
    private final List<BranchContract> contracts = new ArrayList<>();
    private JsonObject consensus;

    public BranchBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public BranchBuilder setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public  BranchBuilder setProperty(String property) {
        this.property = property;
        return this;
    }

    public BranchBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public BranchBuilder setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public BranchBuilder setConsensus(JsonObject consensus) {
        this.consensus = consensus;
        return this;
    }

    public JsonObject buildJson() {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("description", description);
        branch.addProperty("timestamp", timeStamp);

        JsonArray contractArray = new JsonArray();
        contracts.forEach(c -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("contractVersion", c.getContractVersion().toString());
            obj.add("init", c.getInit());
            obj.addProperty("description", c.getDescription());
            obj.addProperty("name", c.getName());
            contractArray.add(obj);
        });

        branch.add("contracts", contractArray);
        branch.add("consensus", consensus);

        return branch;

    }

    public static BranchBuilder builder() {
        return new BranchBuilder();
    }

}
