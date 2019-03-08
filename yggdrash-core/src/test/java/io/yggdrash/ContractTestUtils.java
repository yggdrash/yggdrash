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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
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
        TestConstants.yggdrash();
        return txBodyJson(TestConstants.YEED_CONTRACT,"transfer", params);
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

        final String name = "STEM";
        final String symbol = "STEM";
        final String property = "ecosystem";

        JsonObject contractSample = new JsonObject();
        contractSample.addProperty("contractVersion", TestConstants.STEM_CONTRACT.toString());
        contractSample.add("init", new JsonObject());
        contractSample.addProperty("description", "some description");
        contractSample.addProperty("name", "STEM");

        JsonArray contracts = new JsonArray();
        contracts.add(contractSample);


        return createBranchJson(name, symbol, property, description, contracts, null);
    }

    private static JsonObject createBranchJson(String name,
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

        String consensusString = new StringBuilder()
                .append("{\"consensus\": {")
                .append("    \"algorithm\": \"pbft\",")
                .append("    \"period\": \"2\",")
                .append("    \"validator\": {")
                .append("      \"527e5997e79cc0935d9d86a444380a11cdc296b6bcce2c6df5e5439a3cd7bffb945e77aacf881f36a668284984b628063f5d18a214002ac7ad308e04b67bcad8\": {")
                .append("        \"host\": \"127.0.0.1\",")
                .append("        \"port\": \"32911\"")
                .append("      },")
                .append("      \"e12133df65a2e7dec4310f3511b1fa6b35599770e900ffb50f795f2a49d0a22b63e013a393affe971ea4db08cc491118a8a93719c3c1f55f2a12af21886d294d\": {")
                .append("        \"host\": \"127.0.0.1\",")
                .append("        \"port\": \"32912\"")
                .append("      },")
                .append("      \"8d69860332aa6202df489581fd618fc085a6a5af89964d9e556a398d232816c9618fe15e90015d0a2d15037c91587b79465106f145c0f4db6d18b105659d2bc8\": {")
                .append("        \"host\": \"127.0.0.1\",")
                .append("        \"port\": \"32913\"")
                .append("      },")
                .append("      \"b49fbee055a4b3bd2123a60b24f29d69bc0947e45a75eb4880fe9c5b07904c650729e5edcdaff2523c8839889925079963186bd38c22c96433bdbf4465960527\": {")
                .append("        \"host\": \"127.0.0.1\",")
                .append("        \"port\": \"32914\"")
                .append("      }")
                .append("    }")
                .append("  }}").toString();
        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
        branch.add("consensus", consensus);

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
