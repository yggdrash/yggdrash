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
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
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

        JsonObject json = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(json);
        JsonObject params = createParams(stateValue.getJson());

        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getOwner().toString());

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
            stemContract.genesis(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject branch = getEthToYeedBranch(description);

        String branchId = BranchId.of(branch).toString();
        JsonObject params = new JsonObject();
        params.add(branchId, branch);

        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getOwner().toString());
        try {
            txReceiptField.set(stemContract, receipt);
            receipt = stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertThat(receipt.isSuccess()).isTrue();

        JsonObject saved = stateStore.get(branchId);
        assertThat(saved).isNotNull();
        assertThat(saved.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void updateTest() {
        String description = "Hello World!";
        JsonObject json = ContractTestUtils.createSampleBranchJson(description);
        JsonObject params = createParams(json);

        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(stateValue.getOwner().toString());

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
        JsonObject result = stemContract.view(params);
        assertThat(result.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void getCurrentContractTest() {
        JsonObject params = createParams();
        ContractVersion current = stemContract.getcurrentcontract(params); // No owner validation
        assertThat(current).isEqualTo(stateValue.getContractVersion());
    }

    @Test
    public void getContractHistoryTest() {
        JsonObject params = createParams();
        List<ContractVersion> contractHistory = stemContract.getcontracthistory(params);
        assertThat(contractHistory).containsOnly(stateValue.getContractVersion());
    }

    @Test
    public void getAllBranchIdTest() {
        Set<String> branchIdList = stemContract.getallbranchid();
        assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
    }

    private JsonObject createParams() {
        return ContractTestUtils.createParams(BRANCH_ID, stateValue.getBranchId().toString());
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
        String contractId = "b5790adeafbb9ac6c9be60955484ab1547ab0b76";
        JsonObject genesis = new JsonObject();
        return ContractTestUtils.createBranchJson(name, symbol, property, description,
                contractId, null, genesis);
    }
}