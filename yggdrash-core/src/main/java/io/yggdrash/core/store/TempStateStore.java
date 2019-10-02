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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class TempStateStore implements ReadWriterStore<String, JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(TempStateStore.class);
    private static final String STATE_ROOT = "stateRoot";
    private static final String STATE_HASH = "stateHash";

    private final ReentrantLock lock = new ReentrantLock();

    // Shared Resources(stateStore, tempStore, stateRootHash, stateRootHashBak)
    private final ReadWriterStore<String, JsonObject> stateStore;
    private final Map<String, JsonObject> tempStore = new LinkedHashMap<>();
    private Sha3Hash stateRootHash;
    private Sha3Hash stateRootHashBak;

    public TempStateStore(ReadWriterStore<String, JsonObject> originStore) {
        this.stateStore = originStore;
        setStateRootHash();
    }

    public Sha3Hash getStateRoot() {
        return stateRootHash;
    }

    @Override
    public void put(String key, JsonObject value) {
        lock.lock();
        try {
            tempStore.put(key, value);
            tempStore.put(STATE_ROOT, stateRoot(key, value)); // sateRootObj contains stateHash
        } finally {
            lock.unlock();
        }
    }

    private JsonObject stateRoot(String key, JsonObject value) {
        stateRootHashBak = stateRootHash;
        byte[] changedStateRootByte =  HashUtil.sha3(key.concat(value.toString()).getBytes());

        Sha3Hash newStateRootHash = new Sha3Hash(Bytes.concat(stateRootHash.getBytes(), changedStateRootByte));
        log.trace("stateRoot() key({}) {} -> {}", key, stateRootHash, newStateRootHash);

        this.stateRootHash = newStateRootHash;

        return stateRootObj(newStateRootHash);
    }

    private JsonObject stateRootObj(Sha3Hash stateHash) {
        JsonObject obj = contains(STATE_ROOT) ? get(STATE_ROOT) : new JsonObject();
        obj.addProperty(STATE_HASH, stateHash.toString());
        return obj;
    }

    @Override
    public JsonObject get(String key) {
        try {
            return tempStore.get(key);
        } catch (Exception e1) {
            log.trace("tempStore get() is failed() {}", e1.getMessage());
            try {
                return stateStore.get(key);
            } catch (Exception e2) {
                log.debug("stateStore get() is failed() {}", e2.getMessage());
                return null;
            }
        }
    }

    @Override
    public boolean contains(String key) {
        try {
            return tempStore.containsKey(key);
        } catch (Exception e1) {
            log.trace("tempStore contains() is failed. {}", e1.getMessage());
            try {
                return stateStore.contains(key);
            } catch (Exception e2) {
                log.debug("stateStore contains() is failed. {}", e2.getMessage());
                return false;
            }
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            tempStore.clear();
            setStateRootHash();
        } finally {
            lock.unlock();
        }
    }

    public Set<Map.Entry<String, JsonObject>> changeValues() {
        return this.tempStore.entrySet();
    }

    public void revertStateRootHash() {
        lock.lock();
        try {
            stateRootHash = stateRootHashBak;
        } finally {
            lock.unlock();
        }
    }

    private void setStateRootHash() {
        lock.lock();
        try {
            if (stateStore.contains(STATE_ROOT)) {
                stateRootHash = new Sha3Hash(stateStore.get(STATE_ROOT).get(STATE_HASH).getAsString());
            } else {
                stateRootHash = new Sha3Hash(Constants.EMPTY_HASH);
            }
        } catch (Exception e) {
            log.debug("setStateRootHash() is failed. {}", e.getMessage());
            stateRootHash = new Sha3Hash(Constants.EMPTY_HASH);
        } finally {
            lock.unlock();
        }
    }

}