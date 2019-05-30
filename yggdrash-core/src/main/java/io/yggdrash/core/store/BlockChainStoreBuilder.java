package io.yggdrash.core.store;

import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.BranchId;

public class BlockChainStoreBuilder {
    BranchId branchId;
    private BlockStoreFactory blockStoreFactory;
    private String consensusAlgorithm;
    private boolean isProductionMode;
    private String databasePath;


    private BlockChainStoreBuilder(BranchId branchId) {
        this.branchId = branchId;
    }

    public BlockChainStoreBuilder withProductionMode(Boolean isProductionMode) {
        this.isProductionMode = isProductionMode;
        return this;
    }

    public BlockChainStoreBuilder withDataBasePath(String databasePath) {
        this.databasePath = databasePath;
        return this;
    }


    public BlockChainStoreBuilder setBlockStoreFactory(BlockStoreFactory blockStoreFactory) {
        this.blockStoreFactory = blockStoreFactory;
        return this;
    }

    public BlockChainStoreBuilder setConsensusAlgorithm(String consensusAlgorithm) {
        this.consensusAlgorithm = consensusAlgorithm;
        return this;
    }

    public static BlockChainStoreBuilder newBuilder(BranchId branchId) {
        return new BlockChainStoreBuilder(branchId);
    }


    private DbSource<byte[], byte[]> getDbSource(String name) {
        if (isProductionMode) {
            return new LevelDbDataSource(databasePath, name);
        } else {
            return new HashMapDbSource();
        }
    }

    private BranchStore buildBranchStore() {
        // TODO merge branchStore and StateStore
        return new BranchStore(getDbSource(branchId + "/branch"));
    }

    private TransactionStore buildTransactionStore() {
        return new TransactionStore(getDbSource(branchId + "/txs"));
    }

    private StateStore buildStateStore() {
        return new StateStore(getDbSource(branchId + "/state"));
    }

    private TransactionReceiptStore buildTransactionReceiptStore() {
        return new TransactionReceiptStore(getDbSource(branchId + "/txreceipt"));
    }

    public ConsensusBlockStore buildBlockStore() {
        DbSource dbSource = getDbSource(branchId + "/blocks");
        return blockStoreFactory.create(consensusAlgorithm, dbSource);
    }

    public BlockChainStore build() {
        TransactionStore txStore = buildTransactionStore();
        TransactionReceiptStore txrStore = buildTransactionReceiptStore();
        StateStore stateStore = buildStateStore();
        ConsensusBlockStore blockStore = buildBlockStore();
        BranchStore branchStore = buildBranchStore();

        BlockChainStore blockChainStore = new BlockChainStore(
                txStore,
                txrStore,
                stateStore,
                blockStore,
                branchStore
        );

        return blockChainStore;
    }
}
