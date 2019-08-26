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
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.store.BranchStore;

import java.util.HashMap;
import java.util.Map;

public class BlockChainBuilder {

    private GenesisBlock genesis;
    private Branch branch;
    private BranchStore branchStore;
    private BlockChainManager blockChainManager;
    private ContractManager contractManager;
    private Factory factory;

    public BlockChainBuilder setGenesis(GenesisBlock genesis) {
        this.genesis = genesis;
        return this;
    }

    public BlockChainBuilder setBranchStore(BranchStore branchStore) {
        this.branchStore = branchStore;
        return this;
    }

    public BlockChainBuilder setBlockChainManager(BlockChainManager blockChainManager) {
        this.blockChainManager = blockChainManager;
        return this;
    }

    public BlockChainBuilder setContractManager(ContractManager contractManager) {
        this.contractManager = contractManager;
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
        return factory.create(branch, genesis.getBlock(), branchStore, blockChainManager, contractManager);
    }

    private Map<ContractVersion, Contract> defaultContract() {
        // TODO System Default Contract
        // VersioningContract etc

        // TODO Default Contract has Config
        Map<ContractVersion, Contract> defaultContract = new HashMap<>();

        return defaultContract;

    }

    public static BlockChainBuilder newBuilder() {
        return new BlockChainBuilder();
    }

    public interface Factory {
        BlockChain create(Branch branch,
                          Block genesisBlock,
                          BranchStore branchStore,
                          BlockChainManager blockChainManager,
                          ContractManager contractManager);
    }
}
