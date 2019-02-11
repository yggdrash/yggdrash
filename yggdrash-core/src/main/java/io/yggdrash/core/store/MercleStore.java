package io.yggdrash.core.store;

import io.yggdrash.common.util.SerializeUtil;
import io.yggdrash.core.exception.CreateStoreException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MercleStore implements Store<Object, Object> {
    private static final Logger log = LoggerFactory.getLogger(MercleStore.class);

    private DB db;

    public MercleStore(String path, Options options) {
        if (options == null) {
            options = new Options();
            options.createIfMissing(true);
        }
        try {
            db = Iq80DBFactory.factory.open(new File(path), options);
        } catch (IOException e) {
            throw new CreateStoreException(e.getCause());
        }
    }

    @Override
    public void put(Object key, Object value) {
        db.put(SerializeUtil.serialize(key), SerializeUtil.serialize(value));
    }

    @Override
    public <V> V get(Object key) {
        byte[] value = db.get(SerializeUtil.serialize(key));
        V result = null;
        if (value != null) {
            result = (V) SerializeUtil.deserializeBytes(value);
        }
        return result;
    }

    @Override
    public boolean contains(Object key) {
        byte[] value = db.get(SerializeUtil.serialize(key));
        if (value != null) {
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        try {
            db.close();
        } catch (IOException e) {
            log.error("Failed to find the db file on the close: {}", "mercle store");
        }
    }
}