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

import java.math.BigDecimal;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch {

    private static BigDecimal fee;
    private Long blockHeight;

    public StemContractStateValue(JsonObject json) {
        super(json);
    }

    public void init() {
        setFee(BigDecimal.ZERO);
        setBlockHeight(0L);
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
        getJson().addProperty("fee", fee);
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long height) {
        this.blockHeight = height;
        getJson().addProperty("blockHeight", blockHeight);
    }

    public void updateValidator(String validator) {
        JsonArray validators = getJson().get(("validator")).getAsJsonArray();
        validators.add(validator);
    }

    public void setPreBranchId(String preBranchId) {
        getJson().addProperty("preBranchId", preBranchId);
    }

    public static StemContractStateValue of(JsonObject json) {
        return new StemContractStateValue(json.deepCopy());
    }

}
