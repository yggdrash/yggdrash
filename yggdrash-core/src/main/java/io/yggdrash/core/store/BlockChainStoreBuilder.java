package io.yggdrash.core.store;

import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
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

    private BranchStore buildBranchStore(ReadWriterStore store) {
        // TODO merge branchStore and StateStore
        return new BranchStore(store);
    }

    private TransactionStore buildTransactionStore() {
        return new TransactionStore(getDbSource(branchId + "/txs"));
    }

    private StateStore buildStateStore() {
        return new StateStore(getDbSource(branchId + "/state"));
    }

    private ReceiptStore buildReceiptStore() {
        return new ReceiptStore(getDbSource(branchId + "/receipt"));
    }

    private LogStore buildLogStore() {
        return new LogStore(getDbSource(branchId + "/log"));
    }

    public ConsensusBlockStore buildBlockStore() {
        DbSource dbSource = getDbSource(branchId + "/blocks");
        return blockStoreFactory.create(consensusAlgorithm, dbSource);
    }

    public BlockChainStore build() {
        TransactionStore txStore = buildTransactionStore();
        ReceiptStore receiptStore = buildReceiptStore();
        StateStore stateStore = buildStateStore();
        ConsensusBlockStore blockStore = buildBlockStore();
        // State Store and Branch Store is merged
        StoreAdapter adapter = new StoreAdapter(stateStore, "branch");
        BranchStore branchStore = buildBranchStore(adapter);
        LogStore logStore = buildLogStore();

        BlockChainStore blockChainStore = new BlockChainStore(
                txStore,
                receiptStore,
                stateStore,
                blockStore,
                branchStore,
                logStore
        );

        return blockChainStore;
    }
}
