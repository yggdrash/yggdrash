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

package io.yggdrash.common.store;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;

public class BranchContract {
    private final ContractVersion contractVersion;
    private final JsonObject init;
    private final String name;
    private final String description;
    private final String property;
    private final boolean isSystem;
    private final JsonObject json;

    private BranchContract(JsonObject json) {
        this.init = json.getAsJsonObject("init");
        this.name = json.get("name").getAsString();
        this.description = json.get("description").getAsString();
        this.property = json.has("property") ? json.get("property").getAsString() : "";
        this.isSystem = json.has("isSystem") && json.get("isSystem").getAsBoolean();
        this.contractVersion = ContractVersion.of(json.get("contractVersion").getAsString());
        this.json = json;
    }

    public static BranchContract of(JsonObject json) {
        return new BranchContract(json);
    }

    public ContractVersion getContractVersion() {
        return contractVersion;
    }

    public JsonObject getInit() {
        return init;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProperty() {
        return property;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public JsonObject getJson() {
        return json;
    }
}
