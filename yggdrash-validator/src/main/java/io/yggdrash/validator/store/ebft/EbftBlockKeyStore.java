package io.yggdrash.validator.store.ebft;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.validator.store.BlockKeyStore;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
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
        Options options = new Options();
        options.createIfMissing(true);
        options.compressionType(CompressionType.NONE);
        options.blockSize(10 * 1024 * 1024);
        options.writeBufferSize(10 * 1024 * 1024);
        options.cacheSize(0);
        options.paranoidChecks(true);
        options.verifyChecksums(true);
        options.maxOpenFiles(32);
        this.db = dbSource.init(options);

        try {
            this.size = this.db.getAll().size();
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new NotValidateException("BlockKeyStore is not valid.");
        }
    }

    @Override
    public void put(Long key, byte[] value) {
        if (key < 0 || value == null
                || value.length != Constants.BLOCK_HASH_LENGTH) {
            log.debug("Key or value are not vaild. {} {}", key, value.length);
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

        lock.lock();
        try {
            log.trace("get " + "(" + key + ")");
            return db.get(ByteUtil.longToBytes(key));
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
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
        lock.lock();
        try {
            return this.size;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            this.db.close();
        } finally {
            lock.unlock();
        }
    }
}