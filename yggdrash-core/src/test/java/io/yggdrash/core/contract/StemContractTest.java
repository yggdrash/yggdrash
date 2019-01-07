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
import static io.yggdrash.common.config.Constants.BRANCH_ID;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private StemContractStateValue stateValue;
    private Field txReceiptField;


    @Before
    public void setUp() {
        StateStore<JsonObject> stateStore = new StateStore<>(new HashMapDbSource());
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore(new HashMapDbSource());

        stemContract = new StemContract();
        stemContract.init(stateStore, txReceiptStore);
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(json);
        stemContract.sender = stateValue.getOwner().toString();
        JsonObject params = createParams(stateValue.getJson());

        TransactionReceipt receipt = new TransactionReceipt();

        List<Field> txReceipt = ContractUtils.txReceipt(stemContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }

        try {
            txReceiptField.set(stemContract, receipt);
            stemContract.genesis(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void specification() {
        List<String> methods = stemContract.specification();

        assertThat(methods.isEmpty()).isFalse();
        assertThat(methods.size()).isEqualTo(8);
    }

    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject branch = getEthToYeedBranch(description);

        String branchId = BranchId.of(branch).toString();
        JsonObject params = new JsonObject();
        params.add(branchId, branch);

        TransactionReceipt receipt = new TransactionReceipt();

        try {
            txReceiptField.set(stemContract, receipt);
            receipt = stemContract.create(params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertThat(receipt.isSuccess()).isTrue();

        JsonObject saved = stemContract.state.get(branchId);
        assertThat(saved).isNotNull();
        assertThat(saved.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void updateTest() {
        String description = "Hello World!";
        JsonObject json = ContractTestUtils.createSampleBranchJson(description);
        JsonObject params = createParams(json);

        TransactionReceipt receipt = new TransactionReceipt();

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
    public void searchTest() {
        JsonObject params = new JsonObject();
        params.addProperty("key", "type");
        params.addProperty("value", "immunity");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [type | immunity] res => " + stemContract.search(params));

        params.addProperty("key", "name");
        params.addProperty("value", "TEST1");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [name | TEST1] res => " + stemContract.search(params));

        params.addProperty("key", "property");
        params.addProperty("value", "dex");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [property | dex] res => " + stemContract.search(params));

        params.addProperty("key", "owner");
        params.addProperty("value", "9e187f5264037ab77c87fcffcecd943702cd72c3");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [owner | 9e187f5264037ab77c87fcffcecd943702cd72c3] res => "
                + stemContract.search(params));

        params.addProperty("key", "symbol");
        params.addProperty("value", "TEST1");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [symbol | TEST1] res => " + stemContract.search(params));

        params.addProperty("key", "tag");
        params.addProperty("value", "0.1");

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [tag | 0.1] res => " + stemContract.search(params));
    }

    @Test
    public void getCurrentContractTest() {
        JsonObject params = createParams();
        ContractId current = stemContract.getcurrentcontract(params); // No owner validation
        assertThat(current).isEqualTo(stateValue.getContractId());
    }

    @Test
    public void getContractHistoryTest() {
        JsonObject params = createParams();
        List<ContractId> contractHistory = stemContract.getcontracthistory(params);
        assertThat(contractHistory).containsOnly(stateValue.getContractId());
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