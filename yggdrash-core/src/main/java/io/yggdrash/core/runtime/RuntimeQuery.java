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
import io.yggdrash.core.contract.methods.ContractMethod;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.store.ReadOnlyStore;
import io.yggdrash.core.store.Store;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class RuntimeQuery {
    Contract contract;
    ReadOnlyStore store;
    private Map<String, ContractMethod> queryMethods;

    public RuntimeQuery(Contract contract, Store store) {
        this.contract = ContractUtils.contractInstance(contract);
        this.store = new ReadOnlyStore(store);
        queryMethods = getQueryMethods();

        List<Field> stateField = ContractUtils.stateStoreFields(contract);
        ContractUtils.updateContractFields(this.contract, stateField, this.store);
    }

    public Object query(String method, JsonObject params) throws Exception {
        // Find query method and query
        ContractMethod query = queryMethods.get(method.toLowerCase());
        if (query != null) {
            if (params == null && !query.isParams()) {
                return query.getMethod().invoke(contract);
            } else if(params != null && query.isParams() ) {
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
