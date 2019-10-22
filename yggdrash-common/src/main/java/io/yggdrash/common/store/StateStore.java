package io.yggdrash.common.store;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;

import java.util.concurrent.locks.ReentrantLock;

public class StateStore implements ReadWriterStore<String, JsonObject> {

    private final DbSource<byte[], byte[]> db;
    private long dbSize = 0L;
    private static final byte[] DATABASE_SIZE = "DATABASE_SIZE".getBytes();
    private static final String STATE_ROOT = "stateRoot";
    private static final String STATE_HASH = "stateHash";

    private final ReentrantLock lock = new ReentrantLock();

    public StateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        // getState Size
        if (db.get(DATABASE_SIZE) != null) {
            dbSize = Longs.fromByteArray(db.get(DATABASE_SIZE));
        }
    }

    public long getStateSize() {
        return dbSize;
    }

    public void setLastStateRootHash(String lastStateRootHash) {
        JsonObject obj = new JsonObject();
        obj.addProperty(STATE_HASH, lastStateRootHash);
        put(STATE_ROOT, obj);
    }
    
    @Override
    public void put(String key, JsonObject value) {
        lock.lock();
        try {
            // Check exist
            if (db.get(key.getBytes()) == null) {
                this.dbSize++;
                byte[] dbSizeByteArray = Longs.toByteArray(this.dbSize);
                db.put(DATABASE_SIZE, dbSizeByteArray);
            }
            byte[] tempValue = SerializationUtil.serializeJson(value);
            db.put(key.getBytes(), tempValue);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public JsonObject get(String key) {
        lock.lock();
        try {
            byte[] result = db.get(key.getBytes());
            if (result == null) {
                return null;
            }
            String tempValue = SerializationUtil.deserializeString(result);
            return JsonUtil.parseJsonObject(tempValue);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(String key) {
        lock.lock();
        try {
            return db.get(key.getBytes()) != null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        db.close();
    }


}
