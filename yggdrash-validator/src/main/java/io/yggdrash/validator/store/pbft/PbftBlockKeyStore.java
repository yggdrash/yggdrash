package io.yggdrash.validator.store.pbft;

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class PbftBlockKeyStore implements ReadWriterStore<Long, byte[]> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockKeyStore.class);

    private final DbSource<byte[], byte[]> db;
    private long size = 0;

    public PbftBlockKeyStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(Long key, byte[] value) {
        if (key < 0) {
            log.debug("Key is not vaild.");
            return;
        }

        log.trace("put "
                + "(key: " + key + ")"
                + "(value : " + Hex.toHexString(value) + ")");
        db.put(ByteUtil.longToBytes(key), value);
        size++;
    }

    @Override
    public byte[] get(Long key) {
        if (key < 0) {
            log.debug("Key is not vaild.");
            return null;
        }

        log.trace("get " + "(" + key + ")");
        return db.get(ByteUtil.longToBytes(key));
    }

    @Override
    public boolean contains(Long key) {
        if (key < 0) {
            return false;
        }

        return db.get(ByteUtil.longToBytes(key)) != null;
    }

    public long size() {
        return this.size;
    }

    public void close() {
        this.db.close();
    }
}