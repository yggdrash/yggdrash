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
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.methods.ContractMethod;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.store.TempStateStore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

class RuntimeInvoke {
    // This Class is Invoke Transaction for Contract

    // TODO contract change meta Class
    private final Contract contract;

    private final Map<String, ContractMethod> invokeMethods;
    private Field transactionReceiptField;
    private final List<Field> stateField;

    RuntimeInvoke(Contract contract) {
        // contract Instance
        // TODO change contract to contractMeta
        this.contract = ContractUtils.contractInstance(contract);
        invokeMethods = getInvokeMethods(contract);
        stateField = ContractUtils.stateStoreFields(contract);
        transactionReceipt();
    }

    private void transactionReceipt() {
        // TODO transactionReceipt is required
        for (Field f : ContractUtils.txReceiptFields(contract)) {
            transactionReceiptField = f;
            f.setAccessible(true);
        }
    }

    // TODO Change Meta info
    // TODO filter invoke jSonObject
    private Map<String, ContractMethod> getInvokeMethods(Contract contract) {
        return ContractUtils.contractMethods(contract, InvokeTransaction.class);
    }

    TempStateStore invokeTransaction(JsonObject txBody, TransactionReceipt txReceipt, ReadWriterStore origin)
            throws InvocationTargetException, IllegalAccessException {
        // set State Store
        TempStateStore store = new TempStateStore(origin);
        // set store to contract
        ContractUtils.updateContractFields(contract, stateField, store);
        transactionReceiptField.set(contract, txReceipt);

        // TODO Check Transaction has contractID
        String methodName = txBody.get("method").getAsString();
        ContractMethod method = invokeMethods.get(methodName);
        // filter method exist
        if (method == null) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(errorLog("method is not exist").toString());
            return store;
        }
        // check exist params
        if (txBody.has("params") && method.hasParams()) {
            JsonObject params = txBody.getAsJsonObject("params");
            method.getMethod().invoke(contract, params);
        } else if (!method.hasParams()) {
            method.getMethod().invoke(contract);
        } else {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(errorLog("params is not exist").toString());
            return store;
        }
        return store;
    }

    private JsonObject errorLog(String log) {
        JsonObject errorLog = new JsonObject();
        errorLog.addProperty("error", log);
        return errorLog;
    }


}
