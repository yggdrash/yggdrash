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
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ExecuteStatus;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.contract.methods.ContractMethod;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.TempStateStore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class RuntimeInvoke<T> {
    // This Class is Invoke Transaction for Contract

    // TODO contract change meta Class
    private Contract<T> contract;

    private Map<String, ContractMethod> invokeMethods;
    private ContractMethod genesis;
    private Field transactionReceiptField;
    private List<Field> stateField;

    public RuntimeInvoke(Contract<T> contract) {
        // contract Instance
        // TODO change contract to contractMeta
        this.contract = ContractUtils.contractInstance(contract);
        invokeMethods = getInvokeMethods(contract);
        // Genesis
        //genesis = getGenesisMethod(contract);
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
    private Map<String, ContractMethod> getInvokeMethods(Contract<T> contract) {
        return ContractUtils.contractMethods(contract, InvokeTransaction.class);
    }

    // TODO remove
    private ContractMethod getGenesisMethod(Contract<T> contract) {
        Map<String, ContractMethod> genesisMethods = ContractUtils.contractMethods(contract, Genesis.class);
        Map.Entry<String, ContractMethod> genesisEntry = genesisMethods.isEmpty()
                ? null : genesisMethods.entrySet().iterator().next();

        if (genesisEntry != null) {
            return genesisEntry.getValue();
        }
        return null;
    }

    public TempStateStore invokeTransaction(JsonObject txBody, TransactionReceipt txReceipt, Store origin)
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
        if (txBody.has("params") && method.isParams()) {
            JsonObject params = txBody.getAsJsonObject("params");
            method.getMethod().invoke(contract, params);
        } else if (!method.isParams()) {
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
