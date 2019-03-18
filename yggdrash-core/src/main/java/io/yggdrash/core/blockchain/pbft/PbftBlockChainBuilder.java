/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain.pbft;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.pbft.PbftBlockKeyStore;
import io.yggdrash.core.store.pbft.PbftBlockStore;

public class PbftBlockChainBuilder {

    private BlockChain blockChain;
    private Peer owner;
    private DefaultConfig config;

    public PbftBlockChainBuilder setBlockChain(BlockChain blockChain) {
        this.blockChain = blockChain;
        return this;
    }

    public PbftBlockChainBuilder setOwner(Peer owner) {
        this.owner = owner;
        return this;
    }

    public PbftBlockChainBuilder setConfig(DefaultConfig config) {
        this.config = config;
        return this;
    }

    public PbftBlockChain build() {
        if (blockChain == null) {
            throw new IllegalArgumentException("blockChain is empty");
        }
        if (owner == null) {
            throw new IllegalArgumentException("owner is empty");
        }
        String storePrefix = String.format("%s/pbft/%s_%d", blockChain.getBranchId(), owner.getHost(), owner.getPort());

        String keyStorePath = storePrefix + "/keys";
        String blockStorePath = storePrefix + "/blocks";
        String txStorePath = storePrefix + "/txs";

        PbftBlockKeyStore blockKeyStore = new PbftBlockKeyStore(getDbSource(keyStorePath));
        PbftBlockStore blockStore = new PbftBlockStore(getDbSource(blockStorePath));
        TransactionStore txStore = new TransactionStore(getDbSource(txStorePath));

        PbftBlock genesisBlock = new PbftBlock(blockChain.getBlockByIndex(0).getCoreBlock(), null);
        return new PbftBlockChain(owner, genesisBlock, blockKeyStore, blockStore, txStore);
    }

    private DbSource<byte[], byte[]> getDbSource(String name) {
        if (config != null && config.isProductionMode()) {
            return new LevelDbDataSource(config.getDatabasePath(), name);
        } else {
            return new HashMapDbSource();
        }
    }

    public static PbftBlockChainBuilder Builder() {
        return new PbftBlockChainBuilder();
    }
}
