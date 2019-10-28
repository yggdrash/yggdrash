package io.yggdrash.core.store;

import io.yggdrash.common.store.StateStore;

public class BlockChainStore {
    private final TransactionStore transactionStore;
    private final ReceiptStore receiptStore;
    private final StateStore stateStore;
    private final ConsensusBlockStore consensusBlockStore;
    private final BranchStore branchStore;
    private final ContractStore contractStore;
    private final LogStore logStore;

    public BlockChainStore(TransactionStore transactionStore,
                           ReceiptStore receiptStore,
                           StateStore stateStore,
                           ConsensusBlockStore consensusBlockStore,
                           BranchStore branchStore,
                           LogStore logStore) {
        this.transactionStore = transactionStore;
        this.receiptStore = receiptStore;
        this.stateStore = stateStore;
        this.consensusBlockStore = consensusBlockStore;
        this.branchStore = branchStore;
        this.logStore = logStore;

        contractStore = new ContractStore(branchStore, stateStore, receiptStore);
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public ReceiptStore getReceiptStore() {
        return receiptStore;
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

    public LogStore getLogStore() {
        return logStore;
    }
}
