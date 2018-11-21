package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.exception.FailedOperationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore<T> implements Store<String, T> {

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
                JsonObject jsonObject;
                if (value instanceof JsonObject) {
                    jsonObject = ((JsonObject) value);
                    jsonObject.addProperty("id", entry.getKey().toString());
                } else {
                    jsonObject = new JsonObject();
                    jsonObject.addProperty("id", entry.getKey().toString());
                    jsonObject.addProperty("value", "" + entry.getValue());
                }
                HashMap map = Utils.convertJsonToMap(jsonObject);
                if (map != null) {
                    result.add(map);
                }
            }
        } catch (Exception e) {
            throw new FailedOperationException(e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getStateMap() {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Map.Entry entry : state.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof JsonObject) {
                    JsonObject jsonObject = ((JsonObject) value);
                    HashMap map = Utils.convertJsonToMap(jsonObject);
                    if (map != null) {
                        result.put(entry.getKey().toString(), map);
                    }
                } else {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } catch (Exception e) {
            throw new FailedOperationException(e.getMessage());
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

        Map<String, Map<JsonObject, JsonObject>> tableState = new HashMap<>();
        tableState.put(table, fieldState);
        assetState.put(db, tableState);

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

        Map<JsonObject, JsonObject> fieldState = new HashMap<>();
        fieldState.put(keyObject, recordObject);

        Map<String, Map<JsonObject, JsonObject>> tableState = new HashMap<>();
        tableState.put(table, fieldState);
        assetState.replace(db, tableState);

        return true;
    }

}
