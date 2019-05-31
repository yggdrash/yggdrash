package io.yggdrash.core.store;

import io.yggdrash.common.store.StateStore;

public class BlockChainStore {
    private final TransactionStore transactionStore;
    private final TransactionReceiptStore transactionReceiptStore;
    private final StateStore stateStore;
    private final ConsensusBlockStore consensusBlockStore;
    private final BranchStore branchStore;
    private final ContractStore contractStore;

    public BlockChainStore(TransactionStore transactionStore,
                           TransactionReceiptStore transactionReceiptStore,
                           StateStore stateStore,
                           ConsensusBlockStore consensusBlockStore,
                           BranchStore branchStore) {
        this.transactionStore = transactionStore;
        this.transactionReceiptStore = transactionReceiptStore;
        this.stateStore = stateStore;
        this.consensusBlockStore = consensusBlockStore;
        this.branchStore = branchStore;

        contractStore = new ContractStore(branchStore, stateStore, transactionReceiptStore);
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return transactionReceiptStore;
    }

    public StateStore getStateStore() {
        return stateStore;
    }

    public ConsensusBlockStore getConsensusBlockStore() {
        return consensusBlockStore;
    }

    public BranchStore getBranchStore() {
        return branchStore;
    }

    public ContractStore getContractStore() {
        return contractStore;
    }

}
