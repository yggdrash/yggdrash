package io.yggdrash.core.blockchain;

import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.proto.Proto;

import java.util.Map;

public class BlockMockChain extends BlockChainImpl<Proto.Block, BlockHusk> {

    public BlockMockChain(Branch branch,
                          io.yggdrash.core.blockchain.Block genesisBlock,
                          ConsensusBlockStore<Proto.Block> blockStore,
                          TransactionStore transactionStore,
                          BranchStore branchStore,
                          StateStore stateStore,
                          TransactionReceiptStore transactionReceiptStore,
                          ContractContainer contractContainer,
                          Map<OutputType, OutputStore> outputStores) {
        this(branch, new BlockHusk(genesisBlock), blockStore, transactionStore, branchStore, stateStore,
                transactionReceiptStore, contractContainer, outputStores);
    }

    private BlockMockChain(Branch branch,
                          Block<Proto.Block> genesisBlock,
                          ConsensusBlockStore<Proto.Block> blockStore,
                          TransactionStore transactionStore,
                          BranchStore branchStore,
                          StateStore stateStore,
                          TransactionReceiptStore transactionReceiptStore,
                          ContractContainer contractContainer,
                          Map<OutputType, OutputStore> outputStores) {

        super(branch, genesisBlock, blockStore, transactionStore, branchStore, stateStore,
                transactionReceiptStore, contractContainer, outputStores);
    }

}
