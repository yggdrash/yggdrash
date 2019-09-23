package io.yggdrash.validator.store.pbft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.store.AbstractBlockStore;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.pbft.PbftBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbftBlockStore extends AbstractBlockStore<PbftProto.PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockStore.class);

    public PbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        super(dbSource);
    }

    @Override
    public PbftBlock get(Sha3Hash key) {
        try {
            return new PbftBlock(db.get(key.getBytes()));
        } catch (Exception e) {
            log.debug("get() is failed. {}", e.getMessage());
            return null;
        }
    }
}