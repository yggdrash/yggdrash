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

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockchainMetaInfo;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;

public class StoreBuilder {

    private final DefaultConfig config;

    public StoreBuilder(DefaultConfig config) {
        this.config = config;
    }

    public DefaultConfig getConfig() {
        return config;
    }

    public BlockStore buildBlockStore(BranchId branchId) {
        return new BlockStore(getDbSource(branchId + "/blocks"));
    }

    public TransactionStore buildTxStore(BranchId branchId) {
        return new TransactionStore(getDbSource(branchId + "/txs"));
    }

    public PeerStore buildPeerStore() {
        return new PeerStore(getDbSource("peers"));
    }

    public MetaStore buildMetaStore(BranchId branchId) {
        MetaStore store = new MetaStore(getDbSource(branchId + "/meta"));
        store.put(BlockchainMetaInfo.BRANCH.toString(), branchId.toString());
        return store;
    }

    public StateStore buildStateStore(BranchId branchId) {
        return new StateStore(getDbSource(branchId + "/state"));
    }

    public TransactionReceiptStore buildTransactionReciptStore(BranchId branchId) {
        return new TransactionReceiptStore(getDbSource(branchId + "/txreceipt"));
    }

    protected DbSource<byte[], byte[]> getDbSource(String name) {
        if (config.isProductionMode()) {
            return new LevelDbDataSource(config.getDatabasePath(), name);
        } else {
            return new HashMapDbSource();
        }
    }
}
