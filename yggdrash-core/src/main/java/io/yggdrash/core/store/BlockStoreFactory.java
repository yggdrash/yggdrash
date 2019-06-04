package io.yggdrash.core.store;

import io.yggdrash.common.store.datasource.DbSource;

public interface BlockStoreFactory {
    ConsensusBlockStore create(String consensusAlgorithm, DbSource dbSource);
}
