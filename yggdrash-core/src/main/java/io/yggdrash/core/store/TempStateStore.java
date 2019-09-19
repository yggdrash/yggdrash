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

import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TempStateStore implements ReadWriterStore<String, JsonObject> {
    private final ReadWriterStore<String, JsonObject> stateStore;
    private final Map<String, JsonObject> tempStore = new LinkedHashMap<>();

    private final String STATE_ROOT = "stateRoot";
    private final String STATE_HASH = "stateHash";
    private Sha3Hash stateRootHash;

    public TempStateStore(ReadWriterStore<String, JsonObject> originStore) {
        this.stateStore = originStore;
        setStateRootHash();
    }

    public Sha3Hash getStateRoot() {
        return stateRootHash;
    }

    @Override
    public void put(String key, JsonObject value) {
        tempStore.put(key, value);
        tempStore.put(STATE_ROOT, stateRoot(key, value)); // sateRootObj contains stateHash
    }

    private JsonObject stateRoot(String key, JsonObject value) {
        byte[] changedStateRootByte =  HashUtil.sha3(key.concat(value.toString()).getBytes());
        stateRootHash = new Sha3Hash(Bytes.concat(stateRootHash.getBytes(), changedStateRootByte));
        return stateRootObj(stateRootHash);
    }

    private JsonObject stateRootObj(Sha3Hash stateHash) {
        JsonObject obj = contains(STATE_ROOT) ? get(STATE_ROOT) : new JsonObject();
        obj.addProperty(STATE_HASH, stateHash.toString());
        return obj;
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
        setStateRootHash();
    }

    public void putAll(Set<Map.Entry<String, JsonObject>> values) {
        values.forEach(entry -> tempStore.put(entry.getKey(), entry.getValue()));
    }


    public Set<Map.Entry<String, JsonObject>> changeValues() {
        return this.tempStore.entrySet();
    }

    private void setStateRootHash() {
        if (stateStore.contains(STATE_ROOT)) {
            stateRootHash = new Sha3Hash(stateStore.get(STATE_ROOT).get(STATE_HASH).getAsString());
        } else {
            stateRootHash = new Sha3Hash(Constants.EMPTY_HASH);
        }
    }

}