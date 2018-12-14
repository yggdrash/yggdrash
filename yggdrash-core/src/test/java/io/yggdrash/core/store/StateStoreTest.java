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
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateStoreTest {
    StateStore stateStore;
    Logger log = LoggerFactory.getLogger(StateStoreTest.class);


    @Before
    public void setUp() {
        stateStore = new StateStore(new HashMapDbSource());
    }

    @After
    public void close() {
        stateStore.close();
    }

    @Test
    public void putState() throws Exception {
        String stateKey = "State";
        JsonObject obj = new JsonParser().parse("{\"value\":\"value\"}").getAsJsonObject();
        stateStore.put(stateKey, obj);
        assertTrue(stateStore.getStateSize() == 1L);
    }

    @Test
    public void getState() throws Exception {
        String stateKey = "State";
        JsonObject obj = new JsonParser().parse("{\"value\":\"value\"}").getAsJsonObject();
        stateStore.put(stateKey, obj);
        JsonObject obj2 = stateStore.get(stateKey);
        assertTrue(obj.equals(obj2));
        log.debug(obj.getClass().toString());
    }

    @Test
    public void getStateSize() throws Exception {
        // first is null
        assertTrue(this.stateStore.getStateSize() == 0L);
        // add some state
        this.stateStore.put("STATE", new JsonObject());
        // state size is 1L
        assertTrue(this.stateStore.getStateSize() == 1L);
    }


}