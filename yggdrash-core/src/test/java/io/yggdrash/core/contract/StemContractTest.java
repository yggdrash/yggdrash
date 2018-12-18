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
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StemContractTest {

    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);

    private StemContract stemContract;
    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        StateStore<JsonObject> stateStore = new StateStore<>(new HashMapDbSource());
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore(new HashMapDbSource());

        stemContract = new StemContract();
        stemContract.init(stateStore, txReceiptStore);
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        stateValue = StemContractStateValue.of(json);
        stemContract.sender = stateValue.getOwner().toString();
        JsonObject param = createParam(stateValue.getJson());
        stemContract.genesis(param);
    }

    @Test
    public void specification() {
        List<String> methods = stemContract.specification(null);

        assertThat(methods.isEmpty()).isFalse();
        assertThat(methods.size()).isEqualTo(8);
    }

    @Test
    public void createTest() {
        String description = "ETH TO YEED";
        JsonObject branch = getEthToYeedBranch(description);

        String branchId = BranchId.of(branch).toString();
        JsonObject param = new JsonObject();
        param.add(branchId, branch);

        TransactionReceipt receipt = stemContract.create(param);
        assertThat(receipt.isSuccess()).isTrue();

        JsonObject saved = stemContract.state.get(branchId);
        assertThat(saved).isNotNull();
        assertThat(saved.get("description")).isEqualTo(description);
    }

    @Test
    public void updateTest() {
        String description = "Hello World!";
        JsonObject json = ContractTestUtils.createSampleBranchJson(description);
        JsonObject param = createParam(json);
        assertThat(stemContract.update(param).isSuccess()).isTrue();

        stemBranchViewTest(description);
    }

    private void stemBranchViewTest(String description) {
        JsonObject param = createParam();
        JsonObject result = stemContract.view(param);
        assertThat(result.get("description").getAsString()).isEqualTo(description);
    }

    @Test
    public void searchTest() {
        JsonObject param = new JsonObject();
        param.addProperty("key", "type");
        param.addProperty("value", "immunity");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [type | immunity] res => " + stemContract.search(param));

        param.addProperty("key", "name");
        param.addProperty("value", "TEST1");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [name | TEST1] res => " + stemContract.search(param));

        param.addProperty("key", "property");
        param.addProperty("value", "dex");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [property | dex] res => " + stemContract.search(param));

        param.addProperty("key", "owner");
        param.addProperty("value", "9e187f5264037ab77c87fcffcecd943702cd72c3");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [owner | 9e187f5264037ab77c87fcffcecd943702cd72c3] res => "
                + stemContract.search(param));

        param.addProperty("key", "symbol");
        param.addProperty("value", "TEST1");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [symbol | TEST1] res => " + stemContract.search(param));

        param.addProperty("key", "tag");
        param.addProperty("value", "0.1");

        assertThat(stemContract.search(param)).isNotNull();
        log.debug("Search [tag | 0.1] res => " + stemContract.search(param));
    }

    @Test
    public void getCurrentContractTest() {
        JsonObject param = createParam();
        ContractId current = stemContract.getcurrentcontract(param); // No owner validation
        assertThat(current).isEqualTo(stateValue.getContractId());
    }

    @Test
    public void getContractHistoryTest() {
        JsonObject param = createParam();
        List<ContractId> contractHistory = stemContract.getcontracthistory(param);
        assertThat(contractHistory).containsOnly(stateValue.getContractId());
    }

    private JsonObject createParam() {
        return ContractTestUtils.createParam("branchId", stateValue.getBranchId().toString());
    }

    private JsonObject createParam(JsonElement json) {
        JsonObject param = new JsonObject();
        param.add(stateValue.getBranchId().toString(), json);
        return param;
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