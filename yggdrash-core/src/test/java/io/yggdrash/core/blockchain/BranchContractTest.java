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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.contract.BranchContract;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchContractTest {

    private static final Logger log = LoggerFactory.getLogger(BranchContractTest.class);

    @Test
    public void jsonConvertTest() {
        String contracts = "{\n"
                + "        \"contractVersion\": \"1d35091e51a57a745eec67db3428893968869e32\",\n"
                + "        \"init\": {},\n"
                + "        \"description\": \"The Basis of the YGGDRASH Ecosystem. It is also an "
                + "aggregate and a blockchain containing information of all Branch Chains.\",\n"
                + "        \"name\":\"STEM\"\n"
                + "      }";
        JsonObject contract = (new JsonParser()).parse(contracts).getAsJsonObject();
        BranchContract bc = BranchContract.of(contract);

        log.debug(bc.toString());
        Assert.assertEquals(contract.get("contractVersion").getAsString(), bc.getContractVersion().toString());
        Assert.assertTrue(contract.get("init").isJsonObject());
    }

}
