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
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeTest {

    @Test
    public void yeedRuntimeTest() throws Exception {
        CoinContract contract = new CoinContract();
        Runtime runtime =
                new Runtime<>(
                        new StateStore<>(new HashMapDbSource()),
                        new TransactionReceiptStore(new HashMapDbSource())
                );

        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        JsonObject genesisParams = JsonUtil.parseJsonObject(genesisStr);

        JsonArray txBody = ContractTestUtils.txBodyJson("genesis", genesisParams);
        BranchId branchId = TestConstants.YEED;
        TransactionHusk genesisTx = BlockChainTestUtils.createTxHusk(branchId, txBody);
        assertThat(runtime.invoke(contract, genesisTx)).isTrue();

        JsonObject params = ContractTestUtils.createParams("address",
                "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        BigDecimal result = (BigDecimal)runtime.query(contract, "balanceOf", params);
        assertThat(result).isEqualTo(BigDecimal.valueOf(1000000000));
    }

    @Test
    public void stemRuntimeTest() throws Exception {
        StemContract contract = new StemContract();
        Runtime<JsonObject> runtime =
                new Runtime<>(new StateStore<>(new HashMapDbSource())
                        , new TransactionReceiptStore(new HashMapDbSource()));

        JsonObject json = ContractTestUtils.createSampleBranchJson();
        BranchId branchId = BranchId.of(json);
        TransactionHusk createTx = BlockChainTestUtils.createBranchTxHusk(branchId, "create", json);
        assertThat(runtime.invoke(contract, createTx)).isTrue();

        List<String> result = (List<String>)runtime.query(contract, "getAllBranchId", null);
        assertThat(result).contains(branchId.toString());
    }
}
