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

package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CoinContractTest {

    private CoinContract coinContract;

    @Before
    public void setUp() {
        StateStore<Long> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
        coinContract = new CoinContract();
        coinContract.init(stateStore, txReceiptStore);
        String frontier = TestUtils.wallet().getHexAddress();
        JsonArray params = ContractQry.createParams("frontier", frontier, "balance", "1000000000");
        TransactionReceipt result = coinContract.genesis(params);
        assertThat(result.isSuccess()).isTrue();
        JsonObject balance = coinContract.query(sampleBalanceOfQueryJson(frontier));
        assertThat(balance.get("result").getAsString()).isEqualTo("1000000000");
    }

    @Test
    public void transferTest() {
        TransactionHusk tx = ContractTx.createYeedTx(TestUtils.wallet(), TestUtils.TRANSFER_TO, 100);
        boolean result = coinContract.invoke(tx);
        assertThat(result).isTrue();
        JsonObject balance =
                coinContract.query(sampleBalanceOfQueryJson(TestUtils.TRANSFER_TO.toString()));
        assertThat(balance.get("result").getAsString()).isEqualTo("100");
    }

    private JsonObject sampleBalanceOfQueryJson(String address) {
        JsonObject query = new JsonObject();
        query.addProperty("method", "balanceOf");

        JsonObject param = new JsonObject();
        param.addProperty("address", address);

        JsonArray params = new JsonArray();
        params.add(param);

        query.add("params", params);
        return query;
    }
}
