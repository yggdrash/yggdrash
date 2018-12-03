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
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.store.VisibleStateValue;

import java.util.ArrayList;
import java.util.List;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch implements VisibleStateValue {

    private BranchType type;
    private String tag;
    private final List<ContractId> contractHistory = new ArrayList<>();

    private StemContractStateValue(JsonObject json) {
        super(json);
    }

    public void init() {
        setType("test");
        setTag("0.1");
        updateContractHistory(getContractId());
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

    List<ContractId> getContractHistory() {
        return contractHistory;
    }

    void updateContract(String id) {
        ContractId newContractId = ContractId.of(id);
        if (getContractId().toString().equals(id)) {
            return;
        }

        contractId = newContractId;
        getJson().addProperty("contractId", id);

        updateContractHistory(newContractId);
    }

    private void updateContractHistory(ContractId newContractId) {
        if (contractHistory.contains(newContractId)) {
            return;
        }

        contractHistory.add(newContractId);
        if (!getJson().has("contractHistory")) {
            JsonArray contractHistory = new JsonArray();
            getJson().add("contractHistory", contractHistory);
        }
        getJson().getAsJsonArray("contractHistory").add(newContractId.toString());
    }

    @Override
    public JsonObject getValue() {
        return getJson();
    }

    public static StemContractStateValue of(JsonObject json) {
        return new StemContractStateValue(json);
    }
}
