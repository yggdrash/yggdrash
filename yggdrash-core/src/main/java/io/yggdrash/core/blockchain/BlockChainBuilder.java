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

package io.yggdrash.core.blockchain;

import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ContractClassLoader;
import io.yggdrash.core.contract.ContractMeta;
import io.yggdrash.core.contract.Runtime;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockChainBuilder {
    private static final Logger log = LoggerFactory.getLogger(BlockChainBuilder.class);

    private GenesisBlock genesis;
    private StoreBuilder storeBuilder;

    public BlockChainBuilder addGenesis(GenesisBlock genesis) {
        this.genesis = genesis;
        return this;
    }

    public BlockChainBuilder setStoreBuilder(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
        return this;
    }

    public BlockChain build() {
        BlockHusk genesisBlock = genesis.getBlock();
        BlockStore blockStore = storeBuilder.buildBlockStore(genesisBlock.getBranchId());
        TransactionStore txStore = storeBuilder.buildTxStore(genesisBlock.getBranchId());
        MetaStore metaStore = storeBuilder.buildMetaStore(genesisBlock.getBranchId());

        Contract contract = getContract();
        Runtime<?> runtime = getRunTime(contract.getClass().getGenericSuperclass().getClass());

        return new BlockChain(genesisBlock, blockStore, txStore, metaStore, contract, runtime);
    }

    private Contract getContract() {
        try {
            ContractMeta contractMeta = ContractClassLoader.loadContractById(
                    storeBuilder.getConfig().getContractPath(), genesis.getContractId());
            return contractMeta.getContract().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new FailedOperationException(e);
        }
    }

    private <T> Runtime<T> getRunTime(Class<T> clazz) {
        return new Runtime<>(new StateStore<>(), new TransactionReceiptStore());
    }

    public static BlockChainBuilder Builder() {
        return new BlockChainBuilder();
    }
}
