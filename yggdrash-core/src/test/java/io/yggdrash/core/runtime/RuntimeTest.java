/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.runtime;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.CoinContract;
import io.yggdrash.core.contract.ContractVersion;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import java.math.BigInteger;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RuntimeTest {
    Logger log = LoggerFactory.getLogger(RuntimeTest.class);

    @Test
    public void yeedRuntimeTest() {
        CoinContract contract = new CoinContract();
        ContractVersion coinContract = Constants.YEED_CONTRACT_VERSION;
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
        runtime.addContract(coinContract, contract);

        BranchId branchId = TestConstants.yggdrash();

        TransactionBuilder builder = new TransactionBuilder();
        TransactionHusk testTx = builder.setBranchId(branchId)
                .addTxBody(coinContract, "init", genesisParams, false)
                .setWallet(TestConstants.wallet())
                .build();

        TransactionRuntimeResult result = runtime.invoke(testTx);
        assertThat(result.getReceipt().isSuccess()).isTrue();

        assertThat(result.getChangeValues()
                .get("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94")
                .get("balance").getAsBigInteger()
        ).isEqualTo(BigInteger.valueOf(1000000000));
    }

    @Test
    public void stemRuntimeTest() {
        ContractVersion stemContract = Constants.STEM_CONTRACT_VERSION;

        StemContract contract = new StemContract();
        Runtime<JsonObject> runtime =
                new Runtime<>(
                        new StateStore<>(new HashMapDbSource()),
                        new TransactionReceiptStore(new HashMapDbSource()));
        runtime.addContract(stemContract, contract);

        JsonObject branch = ContractTestUtils.createSampleBranchJson();

        BranchId branchId = BranchId.of(branch);
        JsonObject params = new JsonObject();
        params.add(branchId.toString(), branch);

        TransactionBuilder builder = new TransactionBuilder();
        TransactionHusk testTx = builder.setBranchId(branchId)
                .addTxBody(stemContract, "create", params, false)
                .setWallet(TestConstants.wallet())
                .build();

        log.debug(testTx.toString());
        TransactionRuntimeResult result = runtime.invoke(testTx);
        result.getReceipt().getTxLog().stream().forEach(l -> log.debug(l.toString()));

        assertThat(result.getReceipt().isSuccess()).isTrue();
        System.out.println(result.getChangeValues().get("BRANCH_ID_LIST"));

        assert result.getChangeValues().get("BRANCH_ID_LIST")
                .getAsJsonArray("branchIds")
                .getAsString().contains(branchId.toString());
    }
}
