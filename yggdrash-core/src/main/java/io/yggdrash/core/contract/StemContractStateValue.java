/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;

import java.util.ArrayList;
import java.util.List;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch {

    private BranchType type;
    private String tag;
    private final List<ContractVersion> contractHistory = new ArrayList<>();

    public StemContractStateValue(JsonObject json) {
        super(json);

        if (json.has("type")) {
            this.type = BranchType.of(getJson().get("type").getAsString());
        }
        if (json.has("tag")) {
            this.tag = getJson().get("tag").getAsString();
        }
        if (json.has("contractHistory")) {
            for (JsonElement jsonElement : json.getAsJsonArray("contractHistory")) {
                contractHistory.add(ContractVersion.of(jsonElement.getAsString()));
            }
        }
    }

    public void init() {
        setType("test");
        setTag("0.1");
    }

    public BranchType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = BranchType.of(type);
        getJson().addProperty("type", type);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
        getJson().addProperty("tag", tag);
    }

    public void setDescription(String description) {
        this.description = description;
        getJson().addProperty("description", description);
    }

    public List<ContractVersion> getContractHistory() {
        return contractHistory;
    }

    /*void updateContract(String id) {
        ContractVersion newContractVersion = ContractVersion.of(id);
        if (getContractVersion().toString().equals(id)) {
            return;
        }

        contractVersion = newContractVersion;
        getJson().addProperty("contractVersion", id);

        updateContractHistory(newContractVersion);
    }*/

    private void updateContractHistory(ContractVersion newContractVersion) {
        if (contractHistory.contains(newContractVersion)) {
            return;
        }

        contractHistory.add(newContractVersion);
        if (!getJson().has("contractHistory")) {
            JsonArray contractHistory = new JsonArray();
            getJson().add("contractHistory", contractHistory);
        }
        getJson().getAsJsonArray("contractHistory").add(newContractVersion.toString());
    }

    public static StemContractStateValue of(JsonObject json) {
        return new StemContractStateValue(json.deepCopy());
    }

}
