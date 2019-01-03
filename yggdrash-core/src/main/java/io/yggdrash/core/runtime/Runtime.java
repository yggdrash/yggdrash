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
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public class Runtime<T> {

    private final StateStore<T> stateStore;
    private final TransactionReceiptStore txReceiptStore;
    private Contract<T> contract;

    // FIX runtime run contract will init
    public Runtime(StateStore<T> stateStore, TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.txReceiptStore = txReceiptStore;
    }

    public void setContract(Contract<T> contract){
        this.contract = contract;
        this.contract.init(stateStore, txReceiptStore);
    }

    public boolean invoke(TransactionHusk tx) {
        // TODO fix contract has not call init
//        contract.init(stateStore, txReceiptStore);
        // Find invoke method and invoke
        // validation method
        return contract.invoke(tx);
    }

    public Object query(String method, JsonObject params) throws Exception {
//        contract.init(stateStore, txReceiptStore);
        // Find query method and query
        return contract.query(method, params);
    }

    public StateStore<T> getStateStore() {
        return this.stateStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.txReceiptStore;
    }
}