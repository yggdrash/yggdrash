package io.yggdrash.validator.store.ebft;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.store.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class EbftBlockStore implements BlockStore<byte[], EbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;
    private long size = 0;

    private final ReentrantLock lock = new ReentrantLock();

    public EbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, EbftBlock value) {
        if (key == null || value == null) {
            log.debug("Key or value are not vaild.");
            return;
        }

        byte[] valueBin = value.toBinary();
        if (valueBin.length > Constants.MAX_MEMORY) {
            log.error("Block size is not valid.");
            log.error("put "
                    + "(key: " + Hex.toHexString(key) + ")"
                    + "(value length: " + valueBin.length + ")");
            return;
        }

        lock.lock();

        try {
            if (!contains(key)) {
                log.trace("put "
                        + "(key: " + Arrays.toString(key) + ")"
                        + "(value length: " + valueBin.length + ")");
                db.put(key, valueBin);
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

        log.trace("get " + "(" + Hex.toHexString(key) + ")");
        return new EbftBlock(db.get(key));
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
        return this.size;
    }

    @Override
    public void close() {
        this.db.close();
    }
}