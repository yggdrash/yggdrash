/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchBuilder;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.VALIDATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private StemContractStateValue stateValue;
    private Field txReceiptField;
    private StateStore<JsonObject> stateStore;


    @Before
    public void setUp() throws IllegalAccessException {
        stateStore = new StateStore<>(new HashMapDbSource());

        stemContract = new StemContract();

        JsonObject params = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(params);
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().get(0));
        List<Field> txReceipt = ContractUtils.txReceiptFields(stemContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(stemContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(stemContract, stateStore);
        }


        try {
            txReceiptField.set(stemContract, receipt);
            stemContract.init(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getBranchTest() {
        JsonObject branch = createParams();
        JsonObject json = stemContract.getBranch(branch);
        String branchId = branch.get("branchId").getAsString();
        JsonObject saved = stateStore.get(branchId);
        assertThat(saved.equals(json));
    }

    @Test
    public void getContractByBranchTest() {
        JsonObject branch = createParams();
        Set<JsonElement> contractSet = stemContract.getContractByBranch(branch);
        String branchId = branch.get("branchId").getAsString();
        JsonObject saved = stateStore.get(branchId);
        assertThat(saved.get("contracts").equals(contractSet));
    }

    @Test
    public void getBranchIdByValidatorTest() {
        JsonObject validatorParams = createValidatorParams();
        Set<String> branchIdSet = stemContract.getBranchIdByValidator(validatorParams);
        String validator = validatorParams.get("VALIDATOR").getAsString();

        branchIdSet.stream().forEach(bId -> {
            JsonObject saved = stateStore.get(bId);
            saved.get("validator").getAsJsonArray().forEach(v -> {
                assertThat(v.equals(validator));
            });
        });


    }

    @Test
    public void getValidatorTest() {
        JsonObject branch = createParams();
        Set<String> validatorSet = stemContract.getValidator(branch);
        String branchId = branch.get("branchId").getAsString();
        JsonObject saved = stateStore.get(branchId);
        assertThat(saved.get("validator").equals(validatorSet));
    }

    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject params = getEthToYeedBranch(description);
        BranchId branchId = Branch.of(params).getBranchId();
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().get(0));

        try {
            txReceiptField.set(stemContract, receipt);
            stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertThat(receipt.isSuccess()).isTrue();

        JsonObject saved = stateStore.get(branchId.toString());
        assertThat(saved).isNotNull();
        assertThat(saved.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void updateTest() {
        JsonObject params = createUpdateParams();
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().iterator().next());

        try {
            txReceiptField.set(stemContract, receipt);
            receipt = stemContract.update(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(receipt.isSuccess());
        JsonArray validators = new JsonArray();
        validators.add(params.get("validator").getAsString());
        stemBranchViewTest(validators);
        /* ========================================================= */

        JsonObject params2 = createUpdateParams2();
        TransactionReceipt receipt2 = new TransactionReceiptImpl();
        receipt2.setIssuer(stateValue.getValidators().iterator().next());

        try {
            txReceiptField.set(stemContract, receipt);
            receipt2 = stemContract.update(params2);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(receipt2.isSuccess());
        validators.add(params2.get("validator").getAsString());
        stemBranchViewTest(validators);

    }

    private void stemBranchViewTest(JsonArray validators) {
        JsonObject params = createParams();
        JsonObject branchJson = stemContract.getBranch(params);
        JsonArray uvs= branchJson.get("updateValidators").getAsJsonArray();
        assertEquals(uvs, validators);
    }

    @Test
    public void getBranchList() {
        Set<String> branchIdList = stemContract.getBranchIdList();
        assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
    }

    private JsonObject createParams() {
        return ContractTestUtils.createParams(BRANCH_ID, stateValue.getBranchId().toString());
    }

    private JsonObject createParams(String bid) {
        return ContractTestUtils.createParams(BRANCH_ID, bid);
    }

    private JsonObject createValidatorParams() {
        Optional<String> v = stateValue.getValidators().stream().findFirst();
        return ContractTestUtils.createParams(VALIDATOR, v.get());
    }

    private JsonObject createUpdateParams() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, "ade1be8566f3544dbb58ccfaae61eb45960dbc0d");
        params.addProperty("validator", "30d0c0e7212642b371082df39824c5121c8ad047");
        params.addProperty("fee", BigDecimal.valueOf(1000));
        return params;
    }

    private JsonObject createUpdateParams2() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, "ade1be8566f3544dbb58ccfaae61eb45960dbc0d");
        params.addProperty("validator", "2df39824c5121c8ad04730d0c0e7212642b37108");
        params.addProperty("fee", BigDecimal.valueOf(2000));
        return params;
    }

    private static JsonObject getEthToYeedBranch(String description) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String timeStamp = "00000166c837f0c9";

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

        JsonObject branchJson = BranchBuilder.builder()
                                .setName(name)
                                .setDescription(description)
                                .setSymbol(symbol)
                                .setProperty(property)
                                .setTimeStamp(timeStamp)
                                .addConsensus(consensus)
                                .addValidator(TestConstants.wallet().getHexAddress())
                                .buildJson();

        branchJson.addProperty("fee", BigInteger.valueOf(100));
        return branchJson;
    }

}