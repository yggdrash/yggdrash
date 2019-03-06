package io.yggdrash.core.store.pbft;

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class PbftBlockStore implements ReadWriterStore<byte[], PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;

    public PbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, PbftBlock value) {
        if (key == null || value == null) {
            log.debug("Key or value are not vaild.");
            return;
        }

        log.trace("put "
                + "(key: " + Hex.toHexString(key) + ")"
                + "(value length: " + value.toBinary().length + ")");
        db.put(key, value.toBinary());
    }

    @Override
    public PbftBlock get(byte[] key) {
        if (key == null) {
            log.debug("Key is not vaild.");
            return null;
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