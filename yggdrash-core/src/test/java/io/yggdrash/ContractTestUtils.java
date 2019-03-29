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
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

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

    // TxBody contains multiple transfer transactions.
    public static JsonArray multiTransferTxBodyJson(String to, int length) {
        Set<JsonObject> txObjs = new HashSet<>();
        for (int i = 0; i < length; i++) {
            JsonObject params = new JsonObject();
            params.addProperty("to", to);
            params.addProperty("amount", 100 + i);

            txObjs.add(getTxObj(TestConstants.YEED_CONTRACT.toString(), "transfer", params));
        }

        return txBodyJson(txObjs);
    }

    public static JsonArray txBodyJson(ContractVersion contractVersion, String method, JsonObject params) {
        JsonArray txBody = new JsonArray();
        txBody.add(getTxObj(contractVersion.toString(), method, params));

        return txBody;
    }

    public static JsonArray txBodyJson(Set<JsonObject> txObjs) {
        JsonArray txBody = new JsonArray();
        txObjs.forEach(txBody::add);

        return txBody;
    }

    private static JsonObject getTxObj(String contractVersion, String method, JsonObject params) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("contractVersion", contractVersion.toString());
        txObj.addProperty("method", method);
        txObj.add("params", params);

        return txObj;
    }

    public static JsonObject createSampleBranchJson() {
        String validator = TestConstants.wallet().getHexAddress();
        return createSampleBranchJson(validator);
    }

    public static JsonObject createSampleBranchJson(String validator) {
        TestConstants.yggdrash();

        final String name = "STEM";
        final String symbol = "STEM";
        final String property = "ecosystem";
        final String description = "The Basis of the YGGDRASH Ecosystem."
                + "It is also an aggregate and a blockchain containing information"
                + "of all Branch Chains.";
        final BigDecimal fee = BigDecimal.valueOf(100);

        JsonObject contractSample = new JsonObject();
        contractSample.addProperty("contractVersion", TestConstants.STEM_CONTRACT.toString());
        contractSample.add("init", new JsonObject());
        contractSample.addProperty("description", "some description");
        contractSample.addProperty("name", "STEM");
        contractSample.addProperty("isSystem", true);

        JsonArray contracts = new JsonArray();
        contracts.add(contractSample);


        return createBranchJson(name, symbol, property, description, validator, contracts, fee, null);
    }

    private static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              String validator,
                                              JsonArray contracts,
                                              BigDecimal fee,
                                              String timestamp) {
        JsonObject branchSample = new JsonObject();
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
        validators.add(validator);
        branch.add("validator", validators);

        String consensusString = new StringBuilder()
                .append("{\"consensus\": {\n")
                .append("    \"algorithm\": \"pbft\",\n")
                .append("    \"period\": \"* * * * * *\",\n")
                .append("    \"validator\": [\n")
                .append("      \"77283a04b3410fe21ba5ed04c7bd3ba89e70b78c\",\n")
                .append("      \"9911fb4663637706811a53a0e0b4bcedeee38686\",\n")
                .append("      \"2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312\",\n")
                .append("      \"51e2128e8deb622c2ec6dc38f9d895f0be044eb4\",\n")
                .append("      \"047269a50640ed2b0d45d461488c13abad1e0fac\",\n")
                .append("      \"21640f2116389a3e37462fd6b68b969e490b6a50\",\n")
                .append("      \"63fef4912dc8b0781351b18eb9be450638ea2c17\"\n")
                .append("    ]\n}")
                .append("  }").toString();

        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
        branch.add("consensus", consensus);
        branch.addProperty("fee", fee);

        return branch;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        JsonArray validators = new JsonArray();
        validators.add(wallet.getHexAddress());
        raw.addProperty("validator", wallet.getHexAddress());
        if (!raw.has("signature")) {

            Sha3Hash hashForSign = new Sha3Hash(raw.toString().getBytes(StandardCharsets.UTF_8));
            byte[] signature = wallet.sign(hashForSign.getBytes(), true);
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }
}
