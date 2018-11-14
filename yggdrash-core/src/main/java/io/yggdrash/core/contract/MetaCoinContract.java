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

import java.math.BigDecimal;
import java.util.Map;

public class MetaCoinContract extends CoinContract {

    /**
     * Returns TransactionReceipt (invoke)
     */
    @Override
    public TransactionReceipt genesis(JsonArray params) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getState().size() > 0) {
            return txReceipt;
        }
        log.info("\n genesis :: params => " + params);
        JsonObject json = params.get(0).getAsJsonObject();
        JsonObject alloc = json.get("alloc").getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
            String frontier = entry.getKey();
            JsonObject value = entry.getValue().getAsJsonObject();
            BigDecimal balance = value.get("balance").getAsBigDecimal();
            txReceipt.putLog(frontier, balance);
            state.put(frontier, balance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\nAddress of Frontier : " + frontier
                    + "\nBalance of Frontier : " + balance);
        }
        return txReceipt;
    }
}
