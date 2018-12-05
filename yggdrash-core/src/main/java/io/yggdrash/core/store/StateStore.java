package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore<T> implements Store<String, T> {
    private static final Logger log = LoggerFactory.getLogger(StateStore.class);

    private final Map<String, T> state;
    private final Map<String, Map<Object, Set<Object>>> subState;
    private final Map<String, Map<String, Map<JsonObject, JsonObject>>> assetState;
    private BigDecimal totalSupply = BigDecimal.ZERO;

    public StateStore() {
        this.state = new ConcurrentHashMap<>();
        this.subState = new HashMap<>();
        this.assetState = new HashMap<>();
    }

    public void setTotalSupply(BigDecimal totalSupply) {
        if (this.totalSupply.equals(BigDecimal.ZERO)) {
            this.totalSupply = totalSupply;
        }
    }

    public BigDecimal getTotalSupply() {
        return totalSupply;
    }

    public Map<String, T> getState() {
        return this.state;
    }

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
            //logger.debug(key + " exists in " + subStateKey);
            subState.get(subStateKey).get(key).add(value);
        } else {
            //logger.debug("no " + key + " exists in " + subStateKey);
            Set<Object> newStateValue = new HashSet<>();
            newStateValue.add(value);
            subState.get(subStateKey).put(key, newStateValue);
        }
    }

    public void replace(String key, T value) {
        state.replace(key, value);
    }

    @Override
    public void put(String key, T value) {
        state.put(key, value);
    }

    @Override
    public T get(String key) {
        return state.get(key);
    }

    public Set<T> getAll() {
        Set<T> res = new HashSet<>();
        for (String key : state.keySet()) {
            res.add(state.get(key));
        }
        return res;
    }

    public List<String> getAllKey() {
        return new ArrayList<>(state.keySet());
    }

    @Override
    public boolean contains(String key) {
        return state.containsKey(key);
    }

    @Override
    public void close() {
        state.clear();
        subState.clear();
        assetState.clear();
    }

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
