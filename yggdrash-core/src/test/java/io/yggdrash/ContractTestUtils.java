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
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class ContractTestUtils {

    public static JsonObject createParam(String key, String value) {
        JsonObject param = new JsonObject();
        param.addProperty(key, value);
        return param;
    }

    public static JsonObject createQuery(BranchId branchId, String method, JsonObject param) {
        JsonObject query = new JsonObject();
        query.addProperty("address", branchId.toString());
        query.addProperty("method", method);
        query.add("param", param);
        return query;
    }

    public static JsonArray transferTxBodyJson(String to, long amount) {
        JsonObject param = new JsonObject();
        param.addProperty("to", to);
        param.addProperty("amount", amount);

        return txBodyJson("transfer", param);
    }

    public static JsonArray txBodyJson(String method, JsonObject param) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", method);
        txObj.add("param", param);

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
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String contractId = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
        return createBranchJson(name, symbol, property, description, contractId, null,
                new JsonObject());
    }

    public static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              String contractId,
                                              String timestamp,
                                              JsonObject genesis) {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("description", description);
        branch.addProperty("contractId", contractId);
        branch.add("genesis", genesis);
        if (timestamp == null) {
            branch.addProperty("timestamp", "00000166c837f0c9");
        } else {
            branch.addProperty("timestamp", timestamp);
        }
        signBranch(TestConstants.wallet(), branch);
        return branch;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        if (!raw.has("signature")) {
            raw.addProperty("owner", wallet.getHexAddress());
            Sha3Hash hashForSign = new Sha3Hash(raw.toString().getBytes(StandardCharsets.UTF_8));
            byte[] signature = wallet.signHashedData(hashForSign.getBytes());
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }
}
