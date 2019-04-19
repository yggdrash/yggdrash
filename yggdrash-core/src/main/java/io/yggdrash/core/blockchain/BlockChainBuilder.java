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

import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.blockchain.osgi.ContractContainerBuilder;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.StoreContainer;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;

import java.util.HashMap;
import java.util.Map;

public class BlockChainBuilder {

    private GenesisBlock genesis;
    private StoreBuilder storeBuilder;
    private Branch branch;
    private TransactionStore transactionStore;
    private BranchStore branchStore;
    private ConsensusBlockStore blockStore;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;
    private Map<OutputType, OutputStore> outputStores;
    private Factory factory;

    private ContractPolicyLoader policyLoader;

    public BlockChainBuilder setGenesis(GenesisBlock genesis) {
        this.genesis = genesis;
        return this;
    }

    public BlockChainBuilder setStoreBuilder(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
        return this;
    }

    public BlockChainBuilder setStateStore(StateStore stateStore) {
        this.stateStore = stateStore;
        return this;
    }

    public BlockChainBuilder setPolicyLoader(ContractPolicyLoader policyLoader) {
        this.policyLoader = policyLoader;
        return this;
    }

    public BlockChainBuilder setFactory(Factory factory) {
        this.factory = factory;
        return this;
    }

    public BlockChain build() {
        if (branch == null) {
            branch = genesis.getBranch();
        }
        storeBuilder.setBranchId(branch.getBranchId());
        if (blockStore == null) {
            blockStore = storeBuilder.buildBlockStore();
        }
        if (transactionStore == null) {
            transactionStore = storeBuilder.buildTxStore();
        }
        if (branchStore == null) {
            branchStore = storeBuilder.buildBranchStore();
        }
        if (stateStore == null) {
            stateStore = storeBuilder.buildStateStore();
        }
        if (transactionReceiptStore == null) {
            transactionReceiptStore = storeBuilder.buildTransactionReceiptStore();
        }

        StoreContainer storeContainer = new StoreContainer(branch, branchStore, stateStore, blockStore,
                transactionStore, transactionReceiptStore);

        ContractContainer contractContainer = null;

        if (policyLoader != null) {
            contractContainer = ContractContainerBuilder.newInstance()
                    .withFrameworkFactory(policyLoader.getFrameworkFactory())
                    .withContainerConfig(policyLoader.getContainerConfig())
                    .withBranchId(branch.getBranchId().toString())
                    .withStoreContainer(storeContainer)
                    .withConfig(storeBuilder.getConfig())
                    .build();
        }

        // TODO used storeContainer
        return factory.create(branch, genesis.getBlock(), blockStore,
                transactionStore, branchStore, stateStore, transactionReceiptStore, contractContainer, outputStores);
    }

    private Map<ContractVersion, Contract> defaultContract() {
        // TODO System Default Contract
        // VersionContract etc

        // TODO Default Contract has Config
        Map<ContractVersion, Contract> defaultContract = new HashMap<>();

        return defaultContract;

    }

    public static BlockChainBuilder newBuilder() {
        return new BlockChainBuilder();
    }

    public interface Factory {
        BlockChain create(Branch branch, Block genesisBlock,
                          ConsensusBlockStore blockStore,
                          TransactionStore transactionStore,
                          BranchStore branchStore,
                          StateStore stateStore,
                          TransactionReceiptStore transactionReceiptStore,
                          ContractContainer contractContainer,
                          Map<OutputType, OutputStore> outputStores);
    }
}
