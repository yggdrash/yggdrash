package io.yggdrash.validator.store.pbft;

import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.validator.data.pbft.PbftBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class PbftBlockStore implements Store<byte[], PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;

    public PbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, PbftBlock value) {
        if (key == null || value == null) {
            throw new NotValidateException("Key or value are not vaild.");
        }

        log.trace("put "
                + "(key: " + Hex.toHexString(key) + ")"
                + "(value length: " + value.toBinary().length + ")");
        db.put(key, value.toBinary());
    }

    @Override
    public PbftBlock get(byte[] key) {
        if (key == null) {
            throw new NotValidateException("Key is not vaild.");
        }

        log.trace("get " + "(" + Hex.toHexString(key) + ")");
        return new PbftBlock(db.get(key));
    }

    @Override
    public boolean contains(byte[] key) {
        if (key == null) {
            return false;
        }

        return db.get(key) != null;
    }

    public void close() {
        this.db.close();
    }
}