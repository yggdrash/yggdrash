/*
 * Copyright 2018 Akashic Foundation
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
import com.google.gson.JsonParser;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class StateStoreTest {
    private static final Logger log = LoggerFactory.getLogger(StateStoreTest.class);
    private StateStore stateStore;

    @Before
    public void setUp() {
        stateStore = new StateStore(new HashMapDbSource());
    }

    @After
    public void close() {
        stateStore.close();
    }

    @Test
    public void putState() {
        String stateKey = "State";
        JsonObject obj = new JsonParser().parse("{\"value\":\"value\"}").getAsJsonObject();
        stateStore.put(stateKey, obj);
        assertEquals(stateStore.get(stateKey).get("value").getAsString(), "value");
    }

    @Test
    public void getState() {
        String stateKey = "State";
        JsonObject obj = new JsonParser().parse("{\"value\":\"value\"}").getAsJsonObject();
        stateStore.put(stateKey, obj);
        JsonObject obj2 = stateStore.get(stateKey);
        assertEquals(obj, obj2);
        log.debug(obj.getClass().toString());
    }

}