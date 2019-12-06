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

package io.yggdrash.core.store;

import io.yggdrash.common.store.StateStore;

public class ContractStore {

    private BranchStore branchStore;
    private StateStore stateStore;
    private ReceiptStore receiptStore;
    private TempStateStore tmpStateStore;

    public ContractStore(BranchStore branchStore, StateStore stateStore, ReceiptStore receiptStore) {
        this.branchStore = branchStore;
        this.stateStore = stateStore;
        this.receiptStore = receiptStore;
        this.tmpStateStore = new TempStateStore(stateStore);
    }

    public BranchStore getBranchStore() {
        return this.branchStore;
    }

    public StateStore getStateStore() {
        return this.stateStore;
    }

    public TempStateStore getTmpStateStore() {
        return this.tmpStateStore;
    }

    public ReceiptStore getReceiptStore() {
        return this.receiptStore;
    }

    /*
    public void revertTmpStateStore() {
        this.tmpStateStore = new TempStateStore(stateStore);
    }
    */

    public void close() {
        this.branchStore.close();
        this.stateStore.close();
        this.receiptStore.close();
    }

}
