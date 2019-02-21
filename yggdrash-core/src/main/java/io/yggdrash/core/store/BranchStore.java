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

import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.datasource.DbSource;

public class BranchStore implements Store<BranchId, Branch> {
    DbSource source;


    public BranchStore(DbSource source) {
        source = source;
    }

    @Override
    public void put(BranchId key, Branch value) {
        source.put(key, value);

    }

    @Override
    public boolean contains(BranchId key) {
        return source.get(key) != null;
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public Branch get(BranchId key) {
        return null;
    }

    // TODO Get Branch (Current Version)

    // TODO Get Branch - Version

    // TODO Get Branch - Genesis Version

    // TODO UPDATE Branch - Version History

    // TODO Set Genesis Block

    // TODO Get Genesis Block

    // TODO Get Validator

    // TODO Get Contracts

    // TODO Update Contract

    // TODO Add Contract

    // TODO Set Validator

    // TODO Add Validator

}
