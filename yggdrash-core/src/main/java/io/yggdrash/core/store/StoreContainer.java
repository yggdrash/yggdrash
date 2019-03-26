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
import io.yggdrash.core.blockchain.Branch;

public class StoreContainer {
    Branch branch;

    BranchStore branchStore;
    StateStore stateStore;
    BlockStore blockStore;

    TransactionStore transactionStore;
    TransactionReceiptStore transactionReceiptStore;

    public StoreContainer(Branch branch, BranchStore branchStore, StateStore stateStore, BlockStore blockStore,
                          TransactionStore transactionStore, TransactionReceiptStore transactionReceiptStore) {
        this.branch = branch;
        this.branchStore = branchStore;
        this.stateStore = stateStore;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.transactionReceiptStore = transactionReceiptStore;
    }


    public Branch getBranch() {
        return this.branch;
    }

    public StateStore getStateStore() {
        return this.stateStore;
    }

    public BlockStore getBlockStore() {
        return this.blockStore;
    }

    public TransactionStore getTransactionStore() {
        return this.transactionStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.transactionReceiptStore;
    }






}
