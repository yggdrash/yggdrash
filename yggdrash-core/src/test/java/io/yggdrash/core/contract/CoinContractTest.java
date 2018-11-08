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
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CoinContractTest {
    private static final String TRANSFER_FROM = TestUtils.wallet().getHexAddress();
    private CoinContract coinContract;
    private BranchId branchId = TestUtils.YEED;

    @Before
    public void setUp() {
        StateStore<BigDecimal> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();
        coinContract = new CoinContract();
        coinContract.init(stateStore, txReceiptStore);
        JsonArray params = ContractQry.createParams("frontier", TRANSFER_FROM, "balance", "100");
        TransactionReceipt result = coinContract.genesis(params);
        assertThat(result.isSuccess()).isTrue();
        assertBalanceFromAndTo("100", "0");
    }

    @Test
    public void transferTest() {
        TransactionHusk tx =
                ContractTx.createYeedTx(branchId, TestUtils.wallet(), TestUtils.TRANSFER_TO, 10);
        boolean result = coinContract.invoke(tx);
        assertThat(result).isTrue();
        assertBalanceFromAndTo("90", "10");
    }

    @Test
    public void transferWrongAmountTest() {
        TransactionHusk tx =
                ContractTx.createYeedTx(branchId, TestUtils.wallet(), TestUtils.TRANSFER_TO, 1000);
        boolean result = coinContract.invoke(tx);
        assertThat(result).isFalse();
        assertBalanceFromAndTo("100", "0");

        tx = ContractTx.createYeedTx(branchId, TestUtils.wallet(), TestUtils.TRANSFER_TO, 100);
        result = coinContract.invoke(tx);
        assertThat(result).isTrue();
        assertBalanceFromAndTo("0", "100");
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

    private void assertBalanceFromAndTo(String from, String to) {
        JsonObject fromBalance = coinContract.query(sampleBalanceOfQueryJson(TRANSFER_FROM));
        assertThat(fromBalance.get("result").getAsString()).isEqualTo(from);
        JsonObject toBalance =
                coinContract.query(sampleBalanceOfQueryJson(TestUtils.TRANSFER_TO.toString()));
        assertThat(toBalance.get("result").getAsString()).isEqualTo(to);

    }
}

