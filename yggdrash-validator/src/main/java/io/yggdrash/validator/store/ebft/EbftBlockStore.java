package io.yggdrash.validator.store.ebft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.AbstractBlockStore;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.ebft.EbftBlock;

public class EbftBlockStore extends AbstractBlockStore<EbftProto.EbftBlock> {

    public EbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        super(dbSource);
    }

    @Override
    public EbftBlock get(Sha3Hash key) {
        lock.lock();
        byte[] foundValue = db.get(key.getBytes());
        lock.unlock();
        if (foundValue != null) {
            return new EbftBlock(foundValue);
        }
        throw new NonExistObjectException(key.toString());
    }
}