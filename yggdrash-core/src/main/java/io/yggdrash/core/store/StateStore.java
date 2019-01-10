package io.yggdrash.core.store;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.store.datasource.DbSource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StateStore<T> implements Store<String, JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(StateStore.class);
    private final DbSource<byte[], byte[]> db;
    private long dbSize = 0L;
    private static final byte[] DATABASE_SIZE = "DATABASE_SIZE".getBytes();


    // TODO remote subState and assetState
    private final Map<String, Map<Object, Set<Object>>> subState;

    public StateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        // getState Size
        if (db.get(DATABASE_SIZE) != null) {
            dbSize = Longs.fromByteArray(db.get(DATABASE_SIZE));
        }

        // TODO state is allways key-value store so sub, asset state is remove from contract
        this.subState = new HashMap<>();
    }

    public long getStateSize() {
        return dbSize;
    }

    // TODO remove subState
    public  Map<Object, Set<Object>> getSubState(String key) {
        return this.subState.get(key);
    }

    public void putSubState(String subStateKey, Object key, Object value) {
        if (subState.get(subStateKey) != null) {
            //logger.debug(subStateKey + "State exists");
            updateSubState(subStateKey, key, value);
        } else {
            //logger.debug("no " + subStateKey + "State exists!");
            Set<Object> newStateValue = new HashSet<>();
            newStateValue.add(value);
            Map<Object, Set<Object>> newState = new HashMap<>();
            newState.put(key, newStateValue);
            subState.put(subStateKey, newState);
            //logger.debug(subStateKey + " DB is created");
        }
    }

    private void updateSubState(String subStateKey, Object key, Object value) {
        if (subState.get(subStateKey).get(key) != null) {
            subState.get(subStateKey).get(key).add(value);
        } else {
            Set<Object> newStateValue = new HashSet<>();
            newStateValue.add(value);
            subState.get(subStateKey).put(key, newStateValue);
        }
    }

    @Override
    public void put(String key, JsonObject value) {
        // Check exist
        if (db.get(key.getBytes()) == null) {
            this.dbSize++;
            byte[] dbSizeByteArray = Longs.toByteArray(this.dbSize);
            db.put(DATABASE_SIZE, dbSizeByteArray);
        }
        byte[] tempValue = value.toString().getBytes(StandardCharsets.UTF_8);
        db.put(key.getBytes(), tempValue);
    }

    @Override
    public JsonObject get(String key) {
        byte[] result = db.get(key.getBytes());
        if (result == null) {
            return null;
        }
        String tempValue = new String(result, StandardCharsets.UTF_8);
        return JsonUtil.parseJsonObject(tempValue);
    }

    @Override
    public boolean contains(String key) {
        return db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
        subState.clear();
    }


}
