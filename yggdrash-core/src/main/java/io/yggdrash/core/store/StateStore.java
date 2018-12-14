package io.yggdrash.core.store;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.store.datasource.DbSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StateStore<T> implements Store<String, JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(StateStore.class);
    private byte[] stateValidate = new byte[256];
    private final DbSource<byte[], byte[]> db;
    private long dbSize = 0L;
    private final byte[] DATABASE_SIZE = "DATABASE_SIZE".getBytes();


    private final Map<String, T> state;

    // TODO remote subState and assetState
    private final Map<String, Map<Object, Set<Object>>> subState;
    private final Map<String, Map<String, Map<JsonObject, JsonObject>>> assetState;

    public StateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        // getState Size
        if(db.get(DATABASE_SIZE) != null) {
            dbSize = Longs.fromByteArray(db.get(DATABASE_SIZE));
        }

        // TODO state remove
        this.state = new ConcurrentHashMap<>();
        // must sha256 validator
        this.stateValidate = new byte[256];
        // TODO state is allways key-value store so sub, asset state is remove from contract
        this.subState = new HashMap<>();
        this.assetState = new HashMap<>();
    }

    public T getState(String key) {
        return  this.state.get(key);
        //return this.state;
    }

    public long getStateSize() {
        return dbSize;
    }


    // TODO remove stateList
    public List<Map> getStateList() {
        List<Map> result = new ArrayList<>();
        try {
            for (Map.Entry entry : state.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof VisibleStateValue) {
                    JsonObject jsonObject = ((VisibleStateValue) value).getValue();
                    jsonObject.addProperty("id", entry.getKey().toString());
                    HashMap map = Utils.convertJsonToMap(jsonObject);
                    result.add(map);
                } else {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return result;
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
        if(db.get(key.getBytes()) == null) {
            this.dbSize++;
            byte[] dbSizeByteArray = Longs.toByteArray(this.dbSize);
            db.put(DATABASE_SIZE, dbSizeByteArray);
        }
        byte[] tempValue = value.toString().getBytes();
        db.put(key.getBytes(), tempValue);
    }

    @Override
    public JsonObject get(String key) {
        byte[] result = db.get(key.getBytes());
        if(result == null) {
            return new JsonObject();
        }
        JsonObject obj = new JsonParser().parse(new String(result)).getAsJsonObject();
        return obj;
    }

    // TODO remove getAllKey
    public List<String> getAllKey() {
        return new ArrayList<>(state.keySet());
    }

    @Override
    public boolean contains(String key) {
        return state.containsKey(key);
    }

    @Override
    public void close() {
        db.close();
        state.clear();
        subState.clear();
        assetState.clear();
    }

    // TODO remove all assetState
    public Map<String, Map<JsonObject, JsonObject>> getAssetState(String db) {
        return this.assetState.get(db);
    }

    public Map<JsonObject, JsonObject> getAssetState(String db, String table) {
        return this.assetState.get(db).get(table);
    }

    public JsonObject getAssetState(
            String db, String table, JsonObject keyObject) {
        return this.assetState.get(db).get(table).get(keyObject);
    }

    public boolean putAssetState(
            String db, String table, JsonObject keyObject, JsonObject recordObject) {
        if (db == null || table == null || keyObject == null || recordObject == null) {
            return false;
        }

        try {
            if (assetState.get(db).get(table).get(keyObject) != null) {
                return false;
            }
        } catch (NullPointerException e) {
            // Null point exception.
        }

        Map<JsonObject, JsonObject> fieldState = new HashMap<>();
        fieldState.put(keyObject, recordObject);

        if (assetState.get(db) == null) {
            Map<String, Map<JsonObject, JsonObject>> tableState = new HashMap<>();
            tableState.put(table, fieldState);
            assetState.put(db, tableState);
        } else {
            if (assetState.get(db).get(table) == null) {
                assetState.get(db).put(table, fieldState);
            } else {
                assetState.get(db).get(table).put(keyObject, recordObject);
            }
        }

        return true;
    }

    public boolean updateAssetState(
            String db, String table, JsonObject keyObject, JsonObject recordObject) {
        if (db == null || table == null || keyObject == null || recordObject == null) {
            return false;
        }

        try {
            if (assetState.get(db).get(table).get(keyObject) == null) {
                return false;
            }
        } catch (NullPointerException e) {
            return false;
        }


        if (assetState.get(db) == null) {
            return false;
        } else {
            if (assetState.get(db).get(table) == null) {
                return false;
            } else {
                assetState.get(db).get(table).put(keyObject, recordObject);
            }
        }

        return true;
    }

}
