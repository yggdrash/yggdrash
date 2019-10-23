package io.yggdrash.common.store;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class StateStore implements ReadWriterStore<String, JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StateStore.class);

    private final DbSource<byte[], byte[]> db;
    private static final String STATE_ROOT = "stateRoot";
    private static final String STATE_HASH = "stateHash";

    private final ReentrantLock lock = new ReentrantLock();

    public StateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    public void updatePatch(Map<String, JsonObject> result) {
        if (db instanceof LevelDbDataSource) {
            LevelDbDataSource levelDbDataSource = (LevelDbDataSource) this.db;
            levelDbDataSource.updateByBatch(convertToByteMap(result));
        } else {
            result.forEach(this::put);
        }
    }

    private Map<byte[], byte[]> convertToByteMap(Map<String, JsonObject> result) {
        Map<byte[], byte[]> ret = new HashMap<>();
        for (String k : result.keySet()) {
            ret.put(k.getBytes(), SerializationUtil.serializeJson(result.get(k)));
        }
        return ret;
    }

    @Override
    public void put(String key, JsonObject value) {
        if (key == null || value == null) {
            return;
        }

        lock.lock();
        log.debug("STATEROOT: {} -> {}",
                this.get(STATE_ROOT) == null ? "null" : this.get(STATE_ROOT).get(STATE_HASH).getAsString(),
                value.get(STATE_HASH) == null ? "null" : value.get(STATE_HASH).getAsString());
        try {
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
