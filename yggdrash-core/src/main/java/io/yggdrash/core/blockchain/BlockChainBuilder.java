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
import io.yggdrash.core.contract.CoinContract;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ContractClassLoader;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.contract.ContractMeta;
import io.yggdrash.core.contract.StemContract;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class BlockChainBuilder {

    private GenesisBlock genesis;
    private StoreBuilder storeBuilder;
    private Branch branch;
    private TransactionStore transactionStore;
    private MetaStore metaStore;
    private BlockStore blockStore;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;
    private Runtime runtime;

    public BlockChainBuilder addGenesis(GenesisBlock genesis) {
        this.genesis = genesis;
        return this;
    }

    public BlockChainBuilder setStoreBuilder(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
        return this;
    }

    public BlockChainBuilder setTransactionStore(TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
        return this;
    }

    public BlockChainBuilder setMetaStore(MetaStore metaStore) {
        this.metaStore = metaStore;
        return this;
    }

    public BlockChainBuilder setBlockStore(BlockStore blockStore) {
        this.blockStore = blockStore;
        return this;
    }

    public BlockChainBuilder setStateStore(StateStore stateStore) {
        this.stateStore = stateStore;
        return this;
    }

    public BlockChainBuilder setRuntime(Runtime runtime) {
        this.runtime = runtime;
        return this;
    }

    public BlockChain build() {
        BlockHusk genesisBlock = genesis.getBlock();
        if (branch == null) {
            branch = genesis.getBranch();
        }
        if (blockStore == null) {
            blockStore = storeBuilder.buildBlockStore(genesisBlock.getBranchId());
        }
        if (transactionStore == null) {
            transactionStore = storeBuilder.buildTxStore(genesisBlock.getBranchId());
        }
        if (metaStore == null) {
            metaStore = storeBuilder.buildMetaStore(genesisBlock.getBranchId());
        }
        if (stateStore == null) {
            stateStore = storeBuilder.buildStateStore(genesisBlock.getBranchId());
        }
        if (transactionReceiptStore == null) {
            transactionReceiptStore = storeBuilder.buildTransactionReciptStore(
                    genesisBlock.getBranchId());
        }

        if (runtime == null) {
            runtime = new Runtime(stateStore, transactionReceiptStore);
            // TODO Change Branch Spec
            ContractId branchContractId = branch.getContractId();
            Contract contract;
            // TODO remove branch spec change
            // TODO Get ContractManager for Contract
            if (branch.isStem()) {
                contract = new StemContract();
            } else if (branch.isYeed()) {
                contract = new CoinContract();
            } else {
                contract = getContract(branchContractId);
            }
            runtime.addContract(branchContractId, contract);

            // Add System Contract
            defaultContract().entrySet().forEach(s -> runtime.addContract(s.getKey(),s.getValue()));

        }

        return new BlockChain(branch, genesisBlock, blockStore,
                transactionStore, metaStore, runtime);
    }

    private Contract getContract(ContractId contractId) {
        try {
            // get System Contracts
            // TODO remove this
            // TODO Check System Contract

            ContractMeta contractMeta = ContractClassLoader.loadContractById(
                    storeBuilder.getConfig().getContractPath(), contractId);
            return contractMeta.getContract().getDeclaredConstructor().newInstance();

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new FailedOperationException(e);
        }
    }

    private Map<ContractId, Contract> defaultContract() {
        // TODO System Default Contract
        // VersionContract etc

        // TODO Default Contract has Config
        Map<ContractId, Contract> defaultContract = new HashMap<>();


        return defaultContract;

    }

    public static BlockChainBuilder Builder() {
        return new BlockChainBuilder();
    }
}
