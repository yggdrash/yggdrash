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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private JsonObject jsonObjectBranch;
    private BranchId branchId;

    @Before
    public void setUp() {
        StateStore<JsonObject> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();

        stemContract = new StemContract();
        stemContract.init(stateStore, txReceiptStore);
        jsonObjectBranch = TestUtils.getSampleBranch();
        stemContract.sender = jsonObjectBranch.get("owner").getAsString();
        branchId = BranchId.of(jsonObjectBranch);
        JsonArray params = getInvokeParams(branchId, jsonObjectBranch);
        stemContract.create(params);
    }

    @Test
    public void createTest() {
        JsonObject newBranch = getYeedBranch();
        BranchId newBranchId = BranchId.of(newBranch);
        JsonArray params = getInvokeParams(newBranchId, newBranch);

        assertThat(stemContract.create(params)).isNotNull();
    }

    @Test
    public void updateTest() {
        String description = "Hello World!";
        JsonObject updatedBranch = TestUtils.updateBranch(description, jsonObjectBranch, 0);
        JsonArray params = getInvokeParams(branchId, updatedBranch);
        assertThat(stemContract.update(params).isSuccess()).isTrue();
        viewTest(description);
    }

    private void viewTest(String description) {
        JsonArray params = getQueryParams();
        String json = stemContract.view(params);
        assertThat(json).contains(description); // No owner validation
        log.debug(stemContract.view(params));
    }

    @Test
    public void searchTest() {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("key", "type");
        param.addProperty("value", "immunity");
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [type | immunity] res => " + stemContract.search(params));

        param.addProperty("key", "name");
        param.addProperty("value", "TEST1");
        params.remove(0);
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [name | TEST1] res => " + stemContract.search(params));

        param.addProperty("key", "property");
        param.addProperty("value", "dex");
        params.remove(0);
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [property | dex] res => " + stemContract.search(params));

        param.addProperty("key", "owner");
        param.addProperty("value", "9e187f5264037ab77c87fcffcecd943702cd72c3");
        params.remove(0);
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [owner | 9e187f5264037ab77c87fcffcecd943702cd72c3] res => "
                + stemContract.search(params));

        param.addProperty("key", "symbol");
        param.addProperty("value", "TEST1");
        params.remove(0);
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [symbol | TEST1] res => " + stemContract.search(params));

        param.addProperty("key", "tag");
        param.addProperty("value", "0.1");
        params.remove(0);
        params.add(param);

        assertThat(stemContract.search(params)).isNotNull();
        log.debug("Search [tag | 0.1] res => " + stemContract.search(params));
    }

    @Test
    public void getCurrentVersionTest() {
        JsonArray params = getQueryParams();
        String current = stemContract.getcurrentversion(params); // No owner validation
        String contractId = jsonObjectBranch.get("contractId").getAsString();
        assertThat(current).isEqualTo(contractId);
        log.debug(stemContract.getcurrentversion(params));
    }

    @Test
    public void getVersionHistoryTest() {
        JsonArray params = getQueryParams();
        assertThat(stemContract.getversionhistory(params).size()).isEqualTo(1);
    }

    @Test
    public void getAllBranchIdTest() {
        assertThat(stemContract.getallbranchid(null).size()).isEqualTo(1);
    }

    private JsonArray getInvokeParams(BranchId branchId, JsonObject branch) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.add(branchId.toString(), branch);
        params.add(param);
        return params;
    }

    private JsonArray getQueryParams() {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId.toString());
        params.add(param);
        return params;
    }

    private static JsonObject getYeedBranch() {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String type = "immunity";
        String description = "ETH TO YEED";
        String contractId = "b5790adeafbb9ac6c9be60955484ab1547ab0b76";
        return TestUtils.createBranch(name, symbol, property, type, description, contractId);
    }
}