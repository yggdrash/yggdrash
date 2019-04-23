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
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.core.store.TempStateStore;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;
import static org.junit.Assert.assertTrue;

public class RuntimeInvokeTest {
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private TransactionReceipt txReceipt;
    private BranchId branchId;

    @Test
    public void initTest() throws InvocationTargetException, IllegalAccessException {
        RuntimeInvoke invoke = new RuntimeInvoke(stemContract);

        ReadWriterStore tempStore = new StateStore(new HashMapDbSource());

        JsonObject json = ContractTestUtils.createSampleBranchJson();
        BranchId branchId = Branch.of(json).getBranchId();
        this.branchId = branchId;
        Transaction createTx = BlockChainTestUtils.createBranchTxHusk(branchId, "create", json);
        TransactionReceipt receipt = ContractManager.createTransactionReceipt(createTx);

        this.txReceipt = receipt;
        for (JsonElement txEl: createTx.getBody().getBody()) {
            TempStateStore store = invoke.invokeTransaction(txEl.getAsJsonObject(), receipt, tempStore);
            assertTrue(receipt.isSuccess());
            assertTrue(store.changeValues().size() > 0);
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
