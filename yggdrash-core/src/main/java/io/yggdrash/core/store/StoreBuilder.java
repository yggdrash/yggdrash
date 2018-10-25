/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.core.BranchId;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;

public class StoreBuilder {

    private final boolean isProduction;

    public StoreBuilder(boolean isProduction) {
        this.isProduction = isProduction;
    }

    public BlockStore buildBlockStore(BranchId branchId) {
        return new BlockStore(getDbSource(branchId + "/blocks"));
    }

    public TransactionStore buildTxStore(BranchId branchId) {
        return new TransactionStore(getDbSource(branchId + "/txs"));
    }

    public PeerStore buildPeerStore(BranchId branchId) {
        return new PeerStore(getDbSource(branchId + "/peers"));
    }

    public MetaStore buildMetaStore(BranchId branchId) {
        return new MetaStore(getDbSource(branchId + "/meta"));
    }

    private DbSource<byte[], byte[]> getDbSource(String path) {
        if (isProduction) {
            return new LevelDbDataSource(path);
        } else {
            return new HashMapDbSource();
        }
    }
}
