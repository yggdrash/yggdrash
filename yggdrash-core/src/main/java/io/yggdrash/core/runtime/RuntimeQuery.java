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
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.store.ReadOnlyStore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

class RuntimeQuery {
    private final Contract contract;
    private final Map<String, ContractMethod> queryMethods;

    RuntimeQuery(Contract contract) {
        this.contract = ContractUtils.contractInstance(contract);
        queryMethods = getQueryMethods();
    }

    public void setStore(ReadWriterStore store) {
        ReadOnlyStore readOnlyStore = new ReadOnlyStore(store);
        List<Field> stateField = ContractUtils.stateStoreFields(contract);
        ContractUtils.updateContractFields(this.contract, stateField, readOnlyStore);
    }

    public Object query(String method, JsonObject params) throws InvocationTargetException, IllegalAccessException {
        // Find query method and query
        ContractMethod query = queryMethods.get(method);
        if (query != null) {
            if (params == null && !query.hasParams()) {
                return query.getMethod().invoke(contract);
            } else if (params != null && query.hasParams()) {
                return query.getMethod().invoke(contract, params);
            }

        }
        return null;

    }

    /**
     * Query Method filter
     *
     * @return Method map (method name is lower case)
     */
    private Map<String, ContractMethod> getQueryMethods() {
        return ContractUtils.contractMethods(contract, ContractQuery.class);
    }
}
