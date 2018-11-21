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

import java.util.Map;

public class ContractQry {

    public static JsonObject createQuery(String branchId, String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", branchId);
        query.addProperty("method", method);
        query.add("params", params);

        return query;
    }

    @Deprecated
    public static JsonArray createParams(String key, String value) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty(key, value);
        params.add(param);

        return params;
    }

    @Deprecated
    public static JsonArray createParams(String key1, String value1, String key2, String value2) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty(key1, value1);
        param.addProperty(key2, value2);
        params.add(param);

        return params;
    }

    public static JsonArray createParams(Map<String, String> keyValue) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        if (!keyValue.isEmpty()) {
            for (String key : keyValue.keySet()) {
                param.addProperty(key, keyValue.get(key));
            }
        }
        params.add(param);
        return params;
    }

    static JsonArray createYeedGenesisParams(String key, String value) {
        JsonObject balance = new JsonObject();
        balance.addProperty("balance", value);

        JsonObject alloc = new JsonObject();
        alloc.add(key, balance);

        JsonObject param = new JsonObject();
        param.add("alloc", alloc);

        JsonArray params = new JsonArray();
        params.add(param);

        return params;
    }
}
