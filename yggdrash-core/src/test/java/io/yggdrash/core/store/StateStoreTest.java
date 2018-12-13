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

import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.After;
import static org.junit.Assert.*;
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
    public void putState() {
        String stateKey = "State";
        String stateValue = "value";
        stateStore.put(stateKey, stateValue);
    }

    @Test
    public void getState() {
        String stateKey = "State";
        String stateValue = "value";
        stateStore.put(stateKey, stateValue);
        Object obj = stateStore.get(stateKey);
        log.debug(obj.getClass().toString());
    }

    @Test
    public void getStateSize() {
        assertTrue(this.stateStore.getStateSize() == 0L);
    }


}