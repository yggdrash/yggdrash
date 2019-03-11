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
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.core.store.TempStateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;

public class RuntimeInvokeTest {

    TransactionReceipt txReceipt;
    StemContract stemContract;
    BranchId branchId;

    @Test
    public void initTest() throws InvocationTargetException, IllegalAccessException {
        StemContract contract = new StemContract();
        stemContract = contract;
        RuntimeInvoke invoke = new RuntimeInvoke(contract);

        ReadWriterStore tempStore = new StateStore<>(new HashMapDbSource());

        JsonObject json = ContractTestUtils.createSampleBranchJson();
        BranchId branchId = Branch.of(json).getBranchId();
        this.branchId = branchId;
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

    @Test
    @Ignore
    public void getBranchIdByTxIdTest() {
        JsonObject txParams = createTxParams(txReceipt.getTxId());
        stemContract.getBranchIdByTxId(txParams);
    }

    @Test
    @Ignore
    public void feeStateTest() {
        JsonObject b = createParams();
        stemContract.feeState(b);
    }

    private JsonObject createParams() {
        return ContractTestUtils.createParams(BRANCH_ID, branchId.toString());
    }

    private JsonObject createTxParams(String txId) {
        return ContractTestUtils.createParams(TX_ID, txId);
    }

}
