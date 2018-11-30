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
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        StateStore<StemContractStateValue> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();

        stemContract = new StemContract();
        stemContract.init(stateStore, txReceiptStore);
        stateValue = StemContractStateValue.of(TestUtils.createSampleBranchJson());
        stemContract.sender = stateValue.getOwner().toString();
        JsonArray params =
                ContractTx.createStemParams(stateValue.getBranchId(), stateValue.getJson());
        stemContract.genesis(params);
    }

    @Test
    public void specification() {
        List<String> methods = stemContract.specification(new JsonArray());

        assertTrue(!methods.isEmpty());
        assertEquals(methods.size(), 8);
    }

    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        Branch branch = getEthToYeedBranch(description);
        JsonArray params = ContractTx.createStemParams(branch.getBranchId(), branch.getJson());
        TransactionReceipt receipt = stemContract.create(params);
        assertThat(receipt.isSuccess()).isTrue();

        StemContractStateValue saved = stemContract.state.get(branch.getBranchId().toString());
        assertThat(saved).isNotNull();
        assertThat(saved.getDescription()).isEqualTo(description);
    }

    @Test
    public void updateTest() {
        String description = "Hello World!";
        JsonObject json = TestUtils.createSampleBranchJson(description);
        JsonArray params = ContractTx.createStemParams(stateValue.getBranchId(), json);
        assertThat(stemContract.update(params).isSuccess()).isTrue();

        stemBranchViewTest(description);
    }

    private void stemBranchViewTest(String description) {
        JsonArray params = getQueryParams();
        JsonObject result = stemContract.view(params);
        assertThat(result.get("description").getAsString()).isEqualTo(description);
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
        ContractId current = stemContract.getcurrentversion(params); // No owner validation
        assertThat(current).isEqualTo(stateValue.getContractId());
    }

    @Test
    public void getContractHistoryTest() {
        JsonArray params = getQueryParams();
        List<ContractId> contractHistory = stemContract.getcontracthistory(params);
        assertThat(contractHistory).containsOnly(stateValue.getContractId());
    }

    @Test
    public void getAllBranchIdTest() {
        List<String> branchIdList = stemContract.getallbranchid(null);
        assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
    }

    private JsonArray getQueryParams() {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", stateValue.getBranchId().toString());
        params.add(param);
        return params;
    }

    private static Branch getEthToYeedBranch(String description) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String contractId = "b5790adeafbb9ac6c9be60955484ab1547ab0b76";
        JsonObject json =
                TestUtils.createBranchJson(name, symbol, property, description, contractId, null);
        return Branch.of(json);
    }
}