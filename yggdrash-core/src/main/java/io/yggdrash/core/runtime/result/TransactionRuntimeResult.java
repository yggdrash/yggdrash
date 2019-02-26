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
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.TransactionReceipt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransactionRuntimeResult {
    TransactionHusk tx;
    TransactionReceipt receipt;
    Map<String, JsonObject> changeValues;



    public TransactionRuntimeResult(TransactionHusk tx) {
        this.tx = tx;
        changeValues = new HashMap<>();
    }

    public void setTransactionReceipt(TransactionReceipt receipt) {
        this.receipt = receipt;
    }

    public TransactionReceipt getReceipt() {
        return this.receipt;
    }

    public void setChangeValues(Set<Map.Entry<String, JsonObject>> values) {
        values.forEach(entry -> changeValues.put(entry.getKey(), entry.getValue()));
    }

    public Map<String, JsonObject> getChangeValues() {
        return changeValues;
    }
}
