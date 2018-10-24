/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core;

import io.yggdrash.contract.Contract;
import io.yggdrash.contract.ContractClassLoader;
import io.yggdrash.contract.ContractMeta;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;

public class BlockChainBuilder {

    private BlockHusk genesis;
    private String contractId;
    private boolean productMode = false;

    public BlockChainBuilder addGenesis(BlockHusk genesis) {
        this.genesis = genesis;
        return this;
    }

    // TODO get contractId from genesis
    public BlockChainBuilder addContractId(String contractId) {
        this.contractId = contractId;
        return this;
    }

    public BlockChainBuilder setProductMode(boolean productMode) {
        this.productMode = productMode;
        return this;
    }

    public BlockChain build() throws InstantiationException, IllegalAccessException {
        StoreBuilder storeBuilder = new StoreBuilder(this.productMode);
        BlockStore blockStore = storeBuilder.buildBlockStore(genesis.getBranchId());
        TransactionStore txStore = storeBuilder.buildTxStore(genesis.getBranchId());
        MetaStore metaStore = storeBuilder.buildMetaStore(genesis.getBranchId());

        Contract contract = getContract();
        Runtime<?> runtime = getRunTime(contract.getClass().getGenericSuperclass().getClass());

        return new BlockChain(genesis, blockStore, txStore, metaStore, contract, runtime);
    }

    private Contract getContract()
            throws IllegalAccessException, InstantiationException {
        ContractMeta contractMeta = ContractClassLoader.loadContractById(contractId);
        return contractMeta.getContract().newInstance();
    }

    private <T> Runtime<T> getRunTime(Class<T> clazz) {
        return new Runtime<>(new StateStore<>(), new TransactionReceiptStore());
    }

    public static BlockChainBuilder Builder() {
        return new BlockChainBuilder();
    }
}
