package io.yggdrash.validator.store.ebft;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.store.BlockStore;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class EbftBlockStore implements BlockStore<byte[], EbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;
    private long size;

    private final ReentrantLock lock = new ReentrantLock();

    public EbftBlockStore(DbSource<byte[], byte[]> dbSource) {
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
            throw new NotValidateException("Store is not valid.");
        }
    }

    @Override
    public void put(byte[] key, EbftBlock value) {
        if (key == null || value == null
                || value.toBinary().length > Constants.MAX_MEMORY) {
            log.debug("Key or value are not vaild.");
            return;
        }

        lock.lock();
        try {
            if (!contains(key)) {
                log.trace("put (key: {})(blockHash {})", Hex.toHexString(key), value.getHashHex());
                db.put(key, value.getData());
                size++;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EbftBlock get(byte[] key) {
        if (key == null) {
            log.debug("Key is not vaild.");
            return null;
        }

        lock.lock();
        try {
            log.trace("get ({})", Hex.toHexString(key));
            return new EbftBlock(db.get(key));
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(byte[] key) {
        if (key == null) {
            return false;
        }

        return db.get(key) != null;
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