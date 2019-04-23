/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;

import java.math.BigInteger;

public class CoinContractTestUtils {

    public static JsonObject createTransferBody(String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);

        return ContractTestUtils.txBodyJson(TestConstants.YEED_CONTRACT,"transfer", params);
    }

    public static JsonObject createApproveBody(String spender, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("spender", spender);
        params.addProperty("amount", amount);

        return ContractTestUtils.txBodyJson(TestConstants.YEED_CONTRACT,"approve", params);
    }

    public static JsonObject createTransferFromBody(String from, String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("from", from);
        params.addProperty("to", to);
        params.addProperty("amount", amount);

        return ContractTestUtils.txBodyJson(TestConstants.YEED_CONTRACT,"transferFrom", params);
    }
}
