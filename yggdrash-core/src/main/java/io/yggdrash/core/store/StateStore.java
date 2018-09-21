package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.util.Utils;

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

    public StateStore() {
        this.state = new ConcurrentHashMap<>();
        this.subState = new HashMap<>();
    }

    public Map<String, T> getState() {
        return this.state;
    }

    public List<Map<String, Object>> getStateList() {
        List<Map<String, Object>> result = new ArrayList<>();
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
    }
}
