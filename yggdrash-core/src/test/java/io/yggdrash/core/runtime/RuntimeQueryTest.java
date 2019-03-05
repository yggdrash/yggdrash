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

package io.yggdrash.core.runtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.contract.StemContract;
import org.junit.Ignore;
import org.junit.Test;
import java.math.BigInteger;

@Ignore
public class RuntimeQueryTest {

    @Test
    public void querySomeContract() throws Exception {
        StemContract contract = new StemContract();
        ReadWriterStore<String,JsonObject> tempStore = new StateStore<>(new HashMapDbSource());
        tempStore.put("TOTAL_SUPPLY",
                new JsonParser().parse("{\"balance\":10000}").getAsJsonObject());
        RuntimeQuery query = new RuntimeQuery(contract);
        query.setStore(tempStore);

        Object result = query.query("totalSupply", null);
        assert BigInteger.valueOf(10000).equals(result);
    }
}
