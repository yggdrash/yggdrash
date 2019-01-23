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
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.TempStateStore;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractInvoke<T> {
    // This Class is Invoke Transaction for Contract

    // TODO contract change meta Class
    private Contract<T> contract;

    private Map<String, InvokeMethod> invokeMethods;
    private Method genesis;
    private Field transactionReceiptField;
    private List<Field> stateField;

    public ContractInvoke(Contract<T> contract) {
        // contract Instance
        // TODO change contract to contractMeta
        this.contract = ContractUtils.contractInstance(contract);
        invokeMethods = getInvokeMethods(contract);
        // Genesis
        genesis = getGenesisMethod(contract);
        stateField = ContractUtils.stateStore(contract);
        transactionReceipt();
    }

    private void updateStore(Store stateStore) {
        // init state Store
        for(Field f : stateField) {
            try {
                f.setAccessible(true);
                f.set(contract, stateStore);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void transactionReceipt() {
        // TODO transactionReceipt is required
        for(Field f : ContractUtils.txReceipt(contract)) {
            transactionReceiptField = f;
            f.setAccessible(true);
        }
    }


    // TODO Change Meta info
    // TODO filter invoke jSonObject
    private Map<String,InvokeMethod> getInvokeMethods(Contract<T> contract) {
        Map<String, Method> methods = ContractUtils.contractMethods(contract, InvokeTransction.class);
        Map<String,InvokeMethod> invokeMethods = methods.entrySet()
                .stream()
                .collect(Collectors.toMap(x -> x.getKey(), v -> new InvokeMethod(v.getValue())));
        return invokeMethods;
    }

    // TODO remove
    private Method getGenesisMethod(Contract<T> contract) {
        Map<String, Method> genesisMethods = ContractUtils.contractMethods(contract, Genesis.class);
        Map.Entry<String, Method> genesisEntry = genesisMethods.isEmpty()
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
        updateStore(store);
        transactionReceiptField.set(contract, txReceipt);

        // TODO Check Transaction has contractID
        String methodName = txBody.get("method").getAsString();
        InvokeMethod method = invokeMethods.get(methodName);
        // filter method exist
        if(method == null) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(errorLog("method is not exist"));
            return null;
        }
        TransactionReceipt resultReceipt = null;
        // check exist params
        if(txBody.has("params") && method.isParams()) {
            JsonObject params = txBody.getAsJsonObject("params");
            resultReceipt = (TransactionReceipt) method.getMethod().invoke(contract, params);
        } else {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(errorLog("params is not exist"));
            return null;
        }
        if (txReceipt.getStatus() != ExecuteStatus.SUCCESS) {
            txReceipt.setStatus(resultReceipt.getStatus());
        }
        return store;
    }

    private JsonObject errorLog(String log) {
        JsonObject errorLog = new JsonObject();
        errorLog.addProperty("error", log);
        return errorLog;
    }


}
