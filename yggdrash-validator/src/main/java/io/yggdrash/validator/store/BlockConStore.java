package io.yggdrash.validator.store;

import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.validator.data.BlockCon;
import org.spongycastle.util.encoders.Hex;

public class BlockConStore implements Store<byte[], BlockCon> {
    private final DbSource<byte[], byte[]> db;

    BlockConStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, BlockCon value) {
        db.put(key, value.toBinary());
    }

    @Override
    public BlockCon get(byte[] key) {
        if (key != null) {
            return new BlockCon(key);
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

    public void close() {
        this.db.close();
    }
}