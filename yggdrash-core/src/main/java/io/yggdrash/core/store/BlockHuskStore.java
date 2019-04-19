package io.yggdrash.core.store;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.blockchain.SimpleBlock;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.proto.Proto;

@Deprecated
public class BlockHuskStore extends AbstractBlockStore<Proto.Block> {

    public BlockHuskStore(DbSource<byte[], byte[]> dbSource) {
        super(dbSource);
    }

    @Override
    public ConsensusBlock<Proto.Block> get(Sha3Hash key) {
        lock.lock();
        byte[] foundValue = db.get(key.getBytes());
        lock.unlock();
        if (foundValue != null) {
            return new SimpleBlock(foundValue);
        }
        throw new NonExistObjectException(key.toString());
    }
}