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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.blockchain.BranchBuilder;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.VALIDATOR;
import static org.assertj.core.api.Assertions.assertThat;
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
            stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        JsonObject p = createParams();
//        stemContract.getBranch(p);
        System.out.println(stemContract.getBranch(p));
    }
    @Test
    @Ignore
    public void createTest() {
        // TODO StemContract Change Spec
        String description = "ETH TO YEED";
        JsonObject branch = getEthToYeedBranch(description);

        String branchId = BranchId.of(branch).toString();
        JsonObject params = new JsonObject();
        params.add(branchId, branch);

        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().get(0));

        try {
            txReceiptField.set(stemContract, receipt);
            stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertThat(receipt.isSuccess()).isTrue();

        JsonObject saved = stateStore.get(branchId);
        assertThat(saved).isNotNull();
        assertThat(saved.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    @Ignore
    public void updateTest() {
        String description = "Hello World!";
        JsonObject params = ContractTestUtils.createSampleBranchJson(description);

//        JsonObject params = createParams(json);
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getValidators().iterator().next());

        try {
            txReceiptField.set(stemContract, receipt);
            receipt = stemContract.update(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(receipt.isSuccess());

        stemBranchViewTest(description);
    }

    private void stemBranchViewTest(String description) {
        JsonObject params = createParams();
        JsonObject result = stemContract.getBranch(params);
        assertThat(result.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void getBranchList() {
        Set<String> branchIdList = stemContract.getBranchIdList();
        assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
    }

    private JsonObject createParams() {
        return ContractTestUtils.createParams(BRANCH_ID, stateValue.getBranchId().toString());
    }

    private JsonObject createValidatorParams() {
        Optional<String> v = stateValue.getValidators().stream().findFirst();
        return ContractTestUtils.createParams(VALIDATOR, v.get());
    }

    private JsonObject createParams(JsonElement json) {
        JsonObject params = new JsonObject();
        params.add(stateValue.getBranchId().toString(), json);
        return params;
    }

    private static JsonObject getEthToYeedBranch(String description) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";

        return BranchBuilder.builder()
                .setName(name)
                .setDescription(description)
                .setSymbol(symbol)
                .setProperty(property)
                .addValidator(TestConstants.wallet().getHexAddress())
                .buildJson();
    }

}