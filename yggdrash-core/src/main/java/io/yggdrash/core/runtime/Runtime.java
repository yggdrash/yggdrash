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
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Runtime<T> {

    private final StateStore<T> stateStore;
    private final TransactionReceiptStore txReceiptStore;
    private Contract<T> contract;
    private Map<String, Method> invokeMethod;
    private Map<String, Method> queryMethod;
    private Method genesis;

    // FIX runtime run contract will init
    public Runtime(StateStore<T> stateStore, TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.txReceiptStore = txReceiptStore;
    }

    public void setContract(Contract<T> contract) {
        this.contract = contract;
        this.contract.init(stateStore, txReceiptStore);
        // load invoke Method
        invokeMethod = getInvokeMethods();
        queryMethod = getQueryMethods();
        genesis = getGenesisMethod();

    }

    public boolean invoke(TransactionHusk tx) {
        // TODO fix contract has not call init
        // Find invoke method and invoke
        // validation method
        return contract.invoke(tx);
    }

    public Object query(String method, JsonObject params) throws Exception {
        // Find query method and query
        return contract.query(method, params);
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
        return contractMethods(InvokeTransction.class);
    }

    /**
     * Query Method filter
     *
     * @return Method map (method name is lower case)
     */
    private Map<String, Method> getQueryMethods() {
        return contractMethods(ContractQuery.class);
    }

    private Method getGenesisMethod() {
        Map<String, Method> genesisMethods = contractMethods(Genesis.class);
        Map.Entry<String, Method> genesisEntry = genesisMethods.entrySet().iterator().next();

        if (genesisEntry != null) {
            return genesisEntry.getValue();
        }
        return null;
    }

    private Map<String, Method> contractMethods(Class<? extends Annotation> annotationClass) {
        return Arrays.stream(contract.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationClass))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(Collectors.toMap(m -> m.getName().toLowerCase(), m-> m));
    }



}