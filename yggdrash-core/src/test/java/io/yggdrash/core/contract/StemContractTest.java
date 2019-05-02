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
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;


public class StemContractTest {

    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private StemContractStateValue stateValue;
    private Field txReceiptField;
    private StateStore stateStore;

    @Before
    public void setUp() throws IllegalAccessException {
        stateStore = new StateStore(new HashMapDbSource());

        JsonObject params = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(params);
        TransactionReceipt receipt = createReceipt();
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
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject params = getEthToYeedBranch(description);
        TransactionReceipt receipt = createReceipt();

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
        TransactionReceipt receipt = createReceipt();

        try {
            txReceiptField.set(stemContract, receipt);
            receipt = stemContract.update(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(receipt.isSuccess());
        /* ========================================================= */

        JsonObject params2 = createUpdateParams2();
        TransactionReceipt receipt2 = createReceipt();

        try {
            txReceiptField.set(stemContract, receipt);
            receipt2 = stemContract.update(params2);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(receipt2.isSuccess());
    }

    private JsonObject createUpdateParams() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
        params.addProperty("fee", BigDecimal.valueOf(1000));
        return params;
    }

    private JsonObject createUpdateParams2() {
        JsonObject params = new JsonObject();
        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
        params.addProperty("fee", BigDecimal.valueOf(2000));
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
                .append("    \"period\": \"* * * * * *\"\n")
                .append("   \n}")
                .append("  }").toString();
        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);

        JsonObject branchJson = BranchBuilder.builder()
                                .setName(name)
                                .setDescription(description)
                                .setSymbol(symbol)
                                .setProperty(property)
                                .setTimeStamp(timeStamp)
                                .setConsensus(consensus)
                                .buildJson();

        branchJson.addProperty("fee", BigInteger.valueOf(100));
        return branchJson;
    }

    private TransactionReceipt createReceipt() {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(TestConstants.wallet().getHexAddress());
        return receipt;
    }
}