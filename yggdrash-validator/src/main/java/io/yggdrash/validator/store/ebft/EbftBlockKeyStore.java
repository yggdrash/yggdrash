package io.yggdrash.validator.store.ebft;

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.validator.store.BlockKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class EbftBlockKeyStore implements BlockKeyStore<Long, byte[]> {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockKeyStore.class);

    private final DbSource<byte[], byte[]> db;
    private long size;

    private final ReentrantLock lock = new ReentrantLock();

    public EbftBlockKeyStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        try {
            this.size = this.db.getAll().size();
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new NotValidateException("BlockKeyStore is not valid.");
        }
    }

    @Override
    public void put(Long key, byte[] value) {
        if (key < 0) {
            log.debug("Key is not vaild.");
            return;
        }

        lock.lock();

        try {
            if (!contains(key)) {
                log.trace("put "
                        + "(key: " + key + ")"
                        + "(value : " + Hex.toHexString(value) + ")");
                db.put(ByteUtil.longToBytes(key), value);
                size++;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            lock.unlock();
        }
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

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public void close() {
        this.db.close();
    }
}