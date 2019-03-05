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
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempStateStoreTest {

    StateStore stateStore;
    Logger log = LoggerFactory.getLogger(TempStateStoreTest.class);


    @Before
    public void setUp() {
        stateStore = new StateStore(new HashMapDbSource());
    }

    @After
    public void close() {
        stateStore.close();
    }

    @Test
    public void init() {
        JsonObject originObj = new JsonObject();
        originObj.addProperty("test", "origin");
        stateStore.put("TEST", originObj);
        TempStateStore store = new TempStateStore(stateStore);
        assert store.get("TEST").equals(originObj);

        JsonObject testObj = new JsonObject();
        testObj.addProperty("test", "yes");
        store.put("TEST", testObj);

        assert store.get("TEST").equals(testObj);
        store.close();

        assert store.get("TEST").equals(originObj);
    }
}
