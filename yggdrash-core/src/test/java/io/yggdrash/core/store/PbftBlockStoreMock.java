package io.yggdrash.core.store;

import com.google.common.annotations.VisibleForTesting;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.blockchain.PbftBlockMock;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.proto.PbftProto;

@VisibleForTesting
public class PbftBlockStoreMock extends AbstractBlockStore<PbftProto.PbftBlock> {

    public PbftBlockStoreMock(String consensus, DbSource<byte[], byte[]> dbSource) {
        this(dbSource);
    }

    public PbftBlockStoreMock(DbSource<byte[], byte[]> dbSource) {
        super(dbSource);
    }

    @Override
    public ConsensusBlock<PbftProto.PbftBlock> get(Sha3Hash key) {
        lock.lock();
        byte[] foundValue = db.get(key.getBytes());
        lock.unlock();
        if (foundValue != null) {
            return new PbftBlockMock(foundValue);
        }
        throw new NonExistObjectException(key.toString());
    }
}