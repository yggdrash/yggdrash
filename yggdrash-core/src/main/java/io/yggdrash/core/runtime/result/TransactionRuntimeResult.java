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

package io.yggdrash.core.runtime.result;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransactionRuntimeResult {
    private final Transaction tx;
    private final Map<String, JsonObject> changeValues = new HashMap<>();
    private Receipt receipt;

    public TransactionRuntimeResult(Transaction tx) {
        this.tx = tx;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public Receipt getReceipt() {
        return this.receipt;
    }

    public void setChangeValues(Set<Map.Entry<String, JsonObject>> values) {
        values.forEach(entry -> changeValues.put(entry.getKey(), entry.getValue()));
    }

    public Map<String, JsonObject> getChangeValues() {
        return changeValues;
    }
}