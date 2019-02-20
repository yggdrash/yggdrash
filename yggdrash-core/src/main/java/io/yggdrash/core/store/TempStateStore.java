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

package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TempStateStore implements Store<String, JsonObject> {
    private Store<String, JsonObject> stateStore;
    Map<String, JsonObject> tempStore = new LinkedHashMap<>();

    // TODO check State root
    Sha3Hash stateRoot;

    public TempStateStore(Store originStore) {
        this.stateStore = originStore;
    }

    @Override
    public void put(String key, JsonObject value) {
        tempStore.put(key, value);
        // TODO calculate State Root
    }

    @Override
    public JsonObject get(String key) {
        if (tempStore.get(key) != null) {
            return tempStore.get(key);
        } else {
            return stateStore.get(key);
        }
    }

    @Override
    public boolean contains(String key) {
        if (tempStore.containsKey(key)) {
            return true;
        } else {
            return stateStore.contains(key);
        }
    }

    @Override
    public void close() {
        tempStore.clear();
    }

    public void putAll(Set<Map.Entry<String, JsonObject>> values) {
        values.forEach(entry -> tempStore.put(entry.getKey(), entry.getValue()));
    }

    public Set<Map.Entry<String, JsonObject>> changeValues() {
        return this.tempStore.entrySet();
    }
}
