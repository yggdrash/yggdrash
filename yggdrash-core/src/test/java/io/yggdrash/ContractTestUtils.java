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
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.mock.ContractMock;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.yggdrash.common.config.Constants.BASE_CURRENCY;

public class ContractTestUtils {

    public static JsonObject contractProposeTxBodyJson(String contractVersion) {
        return nodeContractTxBodJson("propose", contractProposeParam(contractVersion));
    }

    public static JsonObject contractVoteTxBodyJson(String txId, boolean agree) {
        return nodeContractTxBodJson("vote", contractVoteParam(txId, agree));
    }

    private static JsonObject nodeContractTxBodJson(String method, JsonObject params) {
        JsonObject txBody = new JsonObject();
        txBody.addProperty("method", method);
        txBody.add("params", params);

        return txBody;
    }

    private static JsonObject contractProposeParam(String contractVersion) {
        JsonObject param = new JsonObject();
        param.addProperty("contractVersion", contractVersion);
        param.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param.addProperty("buildVersion", "1.8.0_172");
        param.addProperty("votingPeriod", 200L);

        return param;
    }

    private static JsonObject contractVoteParam(String txId, boolean agree) {
        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);
        param.addProperty("agree", agree);

        return param;
    }

    public static JsonObject createParams(String key, String value) {
        JsonObject params = new JsonObject();
        params.addProperty(key, value);
        return params;
    }

    public static JsonObject transferTxBodyJson(String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        TestConstants.yggdrash();
        return txBodyJson(TestConstants.YEED_CONTRACT, "transfer", params, true);
    }

    public static JsonObject transferTxBodyJson(String to, BigInteger amount, ContractVersion contractVersion) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        TestConstants.yggdrash();
        return txBodyJson(contractVersion, "transfer", params, true);
    }

    public static JsonObject invalidMethodTransferTxBodyJson(String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        return txBodyJson(TestConstants.YEED_CONTRACT, "create", params, true);
    }

    public static JsonObject createTxBodyJson(JsonObject branch) {
        return stemTxBodyJson("create", branch);
    }

    public static JsonObject updateTxBodyJson(JsonObject branch) {
        return stemTxBodyJson("update", branch);
    }

    private static JsonObject stemTxBodyJson(String method, JsonObject branch) {
        TestConstants.yggdrash();
        return txBodyJson(TestConstants.STEM_CONTRACT, method, branch, false);
    }

    public static JsonObject txBodyJson(
            ContractVersion contractVersion, String method, JsonObject params, boolean isSystem) {
        JsonObject txBody = new JsonObject();
        txBody.addProperty("contractVersion", contractVersion.toString());
        txBody.addProperty("method", method);
        txBody.add("params", params);

        return txBody;
    }

    public static Branch createBranch(ContractMock contractMock) {
        JsonObject branchJson = contractMock.mock();
        return Branch.of(branchJson);
    }

    public static JsonObject createParamForCreateBranch(JsonObject branchObj) {
        JsonObject param = new JsonObject();
        param.add("branch", branchObj);
        param.addProperty("serviceFee", BigInteger.valueOf(100));
        return param;
    }

    public static JsonObject createSampleBranchJson() {
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


        return createBranchJson(name, symbol, property, description, contracts, fee, null);
    }

    private static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              JsonArray contracts,
                                              BigDecimal fee,
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

        String consensusString = new StringBuilder()
                .append("{\"consensus\": {\n")
                .append("    \"algorithm\": \"pbft\",\n")
                .append("    \"period\": \"* * * * * *\"\n")
                .append("    \n}")
                .append("  }").toString();

        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
        branch.add("consensus", consensus);
        branch.addProperty("fee", fee);

        return branch;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        if (!raw.has("signature")) {

            Sha3Hash hashForSign = new Sha3Hash(SerializationUtil.serializeJson(raw));
            byte[] signature = wallet.sign(hashForSign.getBytes(), true);
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }
}
