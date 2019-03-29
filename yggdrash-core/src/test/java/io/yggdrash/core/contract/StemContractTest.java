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
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants.KEY;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private StemContractStateValue stateValue;
    private Field txReceiptField;
    private StateStore stateStore;


    @Before
    public void setUp() throws IllegalAccessException {
        stateStore = new StateStore(new HashMapDbSource());

        JsonObject params = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(params);
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().stream().findFirst().get());
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
    public void getBranchListTest() {
        Set<String> branchIdList = stemContract.getBranchIdList();
        if (!branchIdList.isEmpty()) {
            assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
        }
    }

    @Test
    public void getBranchIdByValidatorTest() {
        JsonObject validatorParams = createValidatorParams();
        if (stemContract.getBranchIdByValidator(validatorParams) != null) {
            Set<String> branchIdSet = stemContract.getBranchIdByValidator(validatorParams);
            String validator = validatorParams.get(KEY.VALIDATOR).getAsString();

            branchIdSet.forEach(bId -> {
                JsonObject saved = stateStore.get(bId);
                saved.get("validator").getAsJsonArray().forEach(v -> assertThat(v).isEqualTo(validator));
            });
        }
    }

    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject params = getEthToYeedBranch(description);
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().stream().findFirst().get());

        try {
            txReceiptField.set(stemContract, receipt);
            stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertThat(receipt.isSuccess()).isTrue();

        BranchId branchId = Branch.of(params).getBranchId();
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

        if (branchJson.has("updateValidators")) {
            JsonArray uvs = branchJson.get("updateValidators").getAsJsonArray();
            assertEquals(uvs, validators);
        }
    }

    private JsonObject createParams() {
        return ContractTestUtils.createParams(BRANCH_ID, stateValue.getBranchId().toString());
    }

    private JsonObject createParams(String bid) {
        return ContractTestUtils.createParams(BRANCH_ID, bid);
    }

    private JsonObject createValidatorParams() {
        Optional<String> v = stateValue.getValidators().stream().findFirst();
        return ContractTestUtils.createParams(KEY.VALIDATOR, v.get());
    }

    private JsonObject createUpdateParams() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
        params.addProperty("validator", "30d0c0e7212642b371082df39824c5121c8ad047");
        params.addProperty("fee", BigDecimal.valueOf(1000));
        return params;
    }

    private JsonObject createUpdateParams2() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
        params.addProperty("validator", "2df39824c5121c8ad04730d0c0e7212642b37108");
        params.addProperty("fee", BigDecimal.valueOf(2000));
        return params;
    }

    private JsonObject createContractParam() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
        params.addProperty("contract", "user-contract-1.0.0.jar");
        return params;
    }

    private static JsonObject getEthToYeedBranch(String description) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String timeStamp = "00000166c837f0c9";

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