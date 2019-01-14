package io.yggdrash.validator.store;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.StoreBuilder;

public class BlockConStoreBuilder extends StoreBuilder {

    public BlockConStoreBuilder(DefaultConfig config) {
        super(config);
    }

    public BlockConStore buildBlockConStore(BranchId branchId) {
        return new BlockConStore(getDbSource(branchId + "/blockcons"));
    }
}
