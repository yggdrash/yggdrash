package io.yggdrash.validator.store;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.datasource.LevelDbDataSource;

public class BlockConStoreBuilder extends StoreBuilder {

    public BlockConStoreBuilder(DefaultConfig config) {
        super(config);
    }

    public BlockConStore buildBlockConStore(BranchId branchId) {
        //todo: check multi blockconstore path
        return new BlockConStore(
                new LevelDbDataSource(this.getConfig().getDatabasePath(),
                        branchId
                                + "/" + System.getProperty("grpc.port")
                                + "/blockcons"));
    }
}
