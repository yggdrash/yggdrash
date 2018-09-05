package io.yggdrash.core.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore<T> implements Store<String, T> {

    private static final Logger logger = LoggerFactory.getLogger(StateStore.class);
    private final Map<String, T> state;
    private final Map<String, Map<Object, Object>> subState;

    public StateStore() {
        this.state = new ConcurrentHashMap<>();
        this.subState = new HashMap<>();
    }

    public Map<String, T> getState() {
        return this.state;
    }

    public  Map<Object, Object> getSubState(String key) {
        return this.subState.get(key);
    }

    public void putSubState(String subStateKey, Object key, Object value) {
        if (subState.get(subStateKey) != null) {
            updateSubState(subStateKey, key, value);
        }
        Map<Object, Object> newState = new HashMap<>();
        newState.put(key, value);
        subState.put(subStateKey, newState);
    }

    private void updateSubState(String subStateKey, Object key, Object value) {
        subState.get(subStateKey).put(key, value);
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

    @Override
    public Set<T> getAll() {
        Set<T> res = new HashSet<>();
        for (String key : state.keySet()) {
            res.add(state.get(key));
        }
        return res;
    }

    public List<String> getAllKey() {
        List<String> branchIdList = new ArrayList<>();
        for (String key : state.keySet()) {
            branchIdList.add(key);
        }
        return branchIdList;
    }

    @Override
    public boolean contains(String key) {
        return state.containsKey(key);
    }
}
