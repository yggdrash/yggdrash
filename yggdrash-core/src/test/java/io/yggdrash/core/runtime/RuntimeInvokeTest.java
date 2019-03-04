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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.TempStateStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static io.yggdrash.common.config.Constants.TX_ID;

public class RuntimeInvokeTest {

    TransactionReceipt txReceipt;

    @Test
    public void initTest() throws InvocationTargetException, IllegalAccessException {
        StemContract contract = new StemContract();
        RuntimeInvoke invoke = new RuntimeInvoke(contract);

        Store tempStore = new StateStore<>(new HashMapDbSource());

        JsonObject json = ContractTestUtils.createSampleBranchJson();
        BranchId branchId = BranchId.of(json.get("branch").getAsJsonObject());

        TransactionHusk createTx = BlockChainTestUtils.createBranchTxHusk(branchId,
                "create", json);
        TransactionReceipt receipt = new TransactionReceiptImpl(createTx);

        this.txReceipt = receipt;
        for (JsonElement txEl: JsonUtil.parseJsonArray(createTx.getBody())) {
            TempStateStore store = invoke.invokeTransaction(
                    txEl.getAsJsonObject(), receipt, tempStore);
            assert receipt.isSuccess();
            assert store.changeValues().size() > 0;
        }
    }

//    @Test
//    public void getBranchIdByTxIdTest() {
//        StemContract contract = new StemContract();
//        JsonObject txParams = createTxParams(txReceipt.getTxId());
////        contract.getBranchIdByTxId(txParams);
//        System.out.println(contract.getBranchIdByTxId(txParams));
//    }

//    @Test
//    public void test() {
//        JsonObject b = createParams();
//        stemContract.feeState(b);
//    }

    private JsonObject createTxParams(String txId) {
        return ContractTestUtils.createParams(TX_ID, txId);
    }

}
