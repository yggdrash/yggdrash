package io.yggdrash.validator.store;

import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.validator.data.EbftBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

public class EbftBlockStore implements Store<byte[], EbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;

    public EbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, EbftBlock value) {
        log.trace("EbftBlockStore put "
                + "(key: " + Hex.toHexString(key) + ")"
                + "(value length: " + value.toBinary().length + ")");
        db.put(key, value.toBinary());
    }

    @Override
    public EbftBlock get(byte[] key) {
        log.trace("EbftBlockStore get "
                + "(" + Hex.toHexString(key) + ")");

        byte[] foundValue = db.get(key);
        if (foundValue != null) {
            log.trace("EbftBlockStore get size: "
                    + foundValue.length);

            return new EbftBlock(foundValue);
        }
        throw new NonExistObjectException("Not Found [" + Hex.toHexString(key) + "]");
    }

    @Override
    public boolean contains(byte[] key) {
        if (key !=  null) {
            return db.get(key) != null;
        }

        return false;
    }

    public int size() {
        try {
            return db.getAll().size();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return 0;
    }

    public void close() {
        this.db.close();
    }
}