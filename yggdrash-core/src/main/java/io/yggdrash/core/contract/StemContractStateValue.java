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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch {

    private static BigDecimal fee;
    private Long blockHeight;
    private final List<ContractVersion> contractHistory = new ArrayList<>();

    public StemContractStateValue(JsonObject json) {
        super(json);
        if (json.has("contractHistory")) {
            for (JsonElement jsonElement : json.getAsJsonArray("contractHistory")) {
                contractHistory.add(ContractVersion.of(jsonElement.getAsString()));
            }
        }
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
        getJson().addProperty("fee", fee);
    }

    public void setBlockHeight(Long height) {
        this.blockHeight = height;
        getJson().addProperty("blockHeight", blockHeight);
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
        if (json.has("fee")) {
            return new StemContractStateValue(json.deepCopy().getAsJsonObject("branch"));
        }
        return new StemContractStateValue(json.deepCopy());
    }

}
