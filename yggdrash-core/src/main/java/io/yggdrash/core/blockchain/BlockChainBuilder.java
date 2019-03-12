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
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.blockchain.osgi.ContractContainerBuilder;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.runtime.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.MetaStore;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.output.OutputStore;
import io.yggdrash.core.store.output.es.EsClient;
import org.apache.commons.lang3.StringUtils;

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
    private OutputStore[] outputStores;

    private ContractPolicyLoader policyLoader;

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

    public BlockChainBuilder setPolicyLoader(ContractPolicyLoader policyLoader) {
        this.policyLoader = policyLoader;
        return this;
    }

    public BlockChain build() {
        BlockHusk genesisBlock = genesis.getBlock();
        if (branch == null) {
            branch = genesis.getBranch();
        }
        BranchId branchId = branch.getBranchId();
        if (blockStore == null) {
            blockStore = storeBuilder.buildBlockStore(branchId);
        }
        if (transactionStore == null) {
            transactionStore = storeBuilder.buildTxStore(branchId);
        }
        if (metaStore == null) {
            metaStore = storeBuilder.buildMetaStore(branchId);
        }
        if (stateStore == null) {
            stateStore = storeBuilder.buildStateStore(branchId);
        }
        if (transactionReceiptStore == null) {
            transactionReceiptStore = storeBuilder.buildTransactionReceiptStore(
                    branchId);
        }

        ContractContainer contractContainer = null;
        if (policyLoader != null) {
            contractContainer = ContractContainerBuilder.newInstance()
                    .withFrameworkFactory(policyLoader.getFrameworkFactory())
                    .withContainerConfig(policyLoader.getContainerConfig())
                    .withBranchId(branch.getBranchId().toString())
                    .withStateStore(stateStore)
                    .withTransactionReceiptStore(transactionReceiptStore)
                    .withConfig(storeBuilder.getConfig())
                    .build();
        }

        if (!StringUtils.isEmpty(System.getProperty("es.host"))
                && !StringUtils.isEmpty(System.getProperty("es.transport"))
                && !StringUtils.isEmpty(System.getProperty("event.store"))) {
            String[] splitHost = System.getProperty("es.host").split(":");
            if (splitHost.length != 2) {
                throw new RuntimeException("The es.host value must be of the form ip:port.");
            }

            if (!StringUtils.isNumeric(System.getProperty("es.transport"))) {
                throw new RuntimeException("The es.transport value must be a number.");
            }

            outputStores = new OutputStore[]{
                    EsClient.newInstance(
                            splitHost[0]
                            , Integer.parseInt(System.getProperty("es.transport"))
                            , System.getProperty("event.store")
                    )
            };
        }

        return new BlockChain(branch, genesisBlock, blockStore,
                transactionStore, metaStore, stateStore, transactionReceiptStore, contractContainer, outputStores);
    }

    private Map<ContractVersion, Contract> defaultContract() {
        // TODO System Default Contract
        // VersionContract etc

        // TODO Default Contract has Config
        Map<ContractVersion, Contract> defaultContract = new HashMap<>();


        return defaultContract;

    }

    public static BlockChainBuilder Builder() {
        return new BlockChainBuilder();
    }
}
