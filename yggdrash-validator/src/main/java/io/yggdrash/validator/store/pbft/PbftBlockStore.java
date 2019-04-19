package io.yggdrash.validator.store.pbft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.AbstractBlockStore;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.pbft.PbftBlock;

public class PbftBlockStore extends AbstractBlockStore<PbftProto.PbftBlock> {

    public PbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        super(dbSource);
    }

    @Override
    public PbftBlock get(Sha3Hash key) {
        lock.lock();
        byte[] foundValue = db.get(key.getBytes());
        lock.unlock();
        if (foundValue != null) {
            return new PbftBlock(foundValue);
        }
        throw new NonExistObjectException(key.toString());
    }
}