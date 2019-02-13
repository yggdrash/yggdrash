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

package io.yggdrash;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.contract.ContractVersion;
import io.yggdrash.core.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;
import java.nio.charset.StandardCharsets;

public class ContractTestUtils {

    public static JsonObject createParams(String key, String value) {
        JsonObject params = new JsonObject();
        params.addProperty(key, value);
        return params;
    }

    public static JsonArray transferTxBodyJson(String to, long amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);

        return txBodyJson(Constants.YEED_CONTRACT_VERSION,"transfer", params);
    }

    public static JsonArray txBodyJson(ContractVersion contractVersion, String method, JsonObject params) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("contractVersion", contractVersion.toString());
        txObj.addProperty("method", method);
        txObj.add("params", params);

        JsonArray txBody = new JsonArray();
        txBody.add(txObj);

        return txBody;
    }

    public static JsonObject createSampleBranchJson() {
        String description =
                "The Basis of the YGGDRASH Ecosystem. "
                        + "It is also an aggregate and a blockchain containing information "
                        + "of all Branch Chains.";
        return createSampleBranchJson(description);
    }

    public static JsonObject createSampleBranchJson(String description) {
        TestConstants.yggdrash();

        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";

        JsonArray contracts = new JsonArray();
        JsonObject contractSample = new JsonObject();
        contractSample.addProperty("contractVersion", TestConstants.STEM_CONTRACT.toString());
        contractSample.add("init", new JsonObject());
        contractSample.addProperty("description", "some description");
        contractSample.addProperty("name", "STEM");
        contracts.add(contractSample);


        return createBranchJson(name, symbol, property, description, contracts, null);
    }

    public static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              JsonArray contracts,
                                              String timestamp) {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("description", description);
        branch.add("contracts", contracts);
        if (timestamp == null) {
            branch.addProperty("timestamp", "00000166c837f0c9");
        } else {
            branch.addProperty("timestamp", timestamp);
        }

        JsonArray validators = new JsonArray();
        validators.add(TestConstants.wallet().getHexAddress());
        branch.add("validator", validators);

        return branch;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        JsonArray validators = new JsonArray();
        validators.add(wallet.getHexAddress());
        raw.addProperty("validator", wallet.getHexAddress());
        if (!raw.has("signature")) {

            Sha3Hash hashForSign = new Sha3Hash(raw.toString().getBytes(StandardCharsets.UTF_8));
            byte[] signature = wallet.signHashedData(hashForSign.getBytes());
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }
}
