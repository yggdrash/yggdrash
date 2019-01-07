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
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runtime<T> {
    protected static final Logger log = LoggerFactory.getLogger(Runtime.class);

    private final StateStore<T> stateStore;
    private final TransactionReceiptStore txReceiptStore;
    private Contract<T> contract;
    private Map<String, Method> invokeMethod;
    private Map<String, Method> queryMethod;
    private Method genesis;
    private Field transactionReceipt;

    // FIX runtime run contract will init
    public Runtime(StateStore<T> stateStore, TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.txReceiptStore = txReceiptStore;
        // init
        queryMethod = new Hashtable<>();
        invokeMethod = new Hashtable<>();
    }

    public void setContract(Contract<T> contract) {
        this.contract = contract;
        this.contract.init(stateStore, txReceiptStore);
        // load invoke Method
        invokeMethod = getInvokeMethods();
        queryMethod = getQueryMethods();
        genesis = getGenesisMethod();
        List<Field> txReceiptList = ContractUtils.txReceipt(contract);
        if (txReceiptList.size() == 1) {
            transactionReceipt = txReceiptList.get(0);
            // TODO contract inject txReceipt
            transactionReceipt.setAccessible(true);
        }

    }


    public void invokeBlock(BlockHusk block) {
        // TODO
        block.getHash();
        block.getBranchId();
        block.getIndex();
        block.getBody();

        // Block Data
        // - Hash
        // - BranchId
        // - Index *(Height)
        // Map String

    }

    private void contractInjectMetaData() {

    }



    public boolean invoke(TransactionHusk tx) {
        // TODO fix contract has not call init
        // Find invoke method and invoke
        // validation method

        // Transaction Data
        // - sender (issuer)
        // - Transaction Hash

        TransactionReceipt txReceipt = new TransactionReceipt();
        String txId = tx.getHash().toString();
        txReceipt.setTxId(txId);
        // TODO insert txReceipt BlockID
        //txReceipt.setBlockId("");
        try {
            if (transactionReceipt != null) {
                transactionReceipt.set(contract, txReceipt);
            }

            // this.sender = tx.getAddress().toString();

            JsonObject txBody = JsonUtil.parseJsonArray(tx.getBody()).get(0).getAsJsonObject();

            //dataFormatValidation(txBody);

            String methodName = txBody.get("method").getAsString().toLowerCase();
            Method method = invokeMethod.get(methodName);
            // TODO Inject Transction Receipt
            if (method != null) {
                if (txBody.has("params")) {
                    JsonObject params = txBody.getAsJsonObject("params");
                    // TODO how to make more simple
                    Optional<Class<?>> m = Arrays.stream(method.getParameterTypes())
                            .filter(p -> p == JsonObject.class).findFirst();
                    if (method.getParameterCount() == 1 && m.isPresent()) {
                        txReceipt = (TransactionReceipt) method.invoke(contract, params);
                    } else {
                      // Make Exception
                    }
                } else {
                    txReceipt = (TransactionReceipt) method.invoke(contract);
                }

            } else {
                // TODO Make Exception
            }


            txReceipt.putLog("method", methodName);
            txReceipt.setTxId(txId);
        } catch (Throwable e) {
            txReceipt = TransactionReceipt.errorReceipt(txId, e);
        }
        // save Tranction Receipt
        txReceiptStore.put(txReceipt.getTxId(), txReceipt);
        return txReceipt.isSuccess();

    }

    public Object query(String method, JsonObject params) throws Exception {
        // Find query method and query
        Method query = queryMethod.get(method.toLowerCase());
        if (query != null) {
            if (params == null) {
                return query.invoke(contract);
            } else {
                return query.invoke(contract, params);
            }

        }
        return null;

    }

    public StateStore<T> getStateStore() {
        return this.stateStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.txReceiptStore;
    }

    /**
     * Invoke Method filter
     * @return Method map (method nams is lower case)
     */
    private Map<String,Method> getInvokeMethods() {
        return ContractUtils.contractMethods(contract, InvokeTransction.class);
    }

    /**
     * Query Method filter
     *
     * @return Method map (method name is lower case)
     */
    private Map<String, Method> getQueryMethods() {
        return ContractUtils.contractMethods(contract, ContractQuery.class);
    }

    private Method getGenesisMethod() {
        Map<String, Method> genesisMethods = ContractUtils.contractMethods(contract, Genesis.class);
        Map.Entry<String, Method> genesisEntry = genesisMethods.isEmpty()
                ? null : genesisMethods.entrySet().iterator().next();

        if (genesisEntry != null) {
            return genesisEntry.getValue();
        }
        return null;
    }

}