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
    private final Map<String, Map<Object, Set<Object>>> subState;

    public StateStore() {
        this.state = new ConcurrentHashMap<>();
        this.subState = new HashMap<>();
    }

    public Map<String, T> getState() {
        return this.state;
    }

    public  Map<Object, Set<Object>> getSubState(String key) {
        return this.subState.get(key);
    }

    public void putSubState(String subStateKey, Object key, Object value) {
        if (subState.get(subStateKey) != null) {
            logger.debug(subStateKey + "State exists! :)");
            updateSubState(subStateKey, key, value);
        } else {
            logger.debug("no " + subStateKey + "State exists! :(");
            Set<Object> newStateValue = new HashSet<>();
            newStateValue.add(value);
            Map<Object, Set<Object>> newState = new HashMap<>();
            newState.put(key, newStateValue);
            subState.put(subStateKey, newState);
            logger.debug(subStateKey + " DB is created");
        }
    }

    private void updateSubState(String subStateKey, Object key, Object value) {
        if (subState.get(subStateKey).get(key) != null) {
            logger.debug(key + " exists in " + subStateKey + ":)");
            subState.get(subStateKey).get(key).add(value);
        } else {
            logger.debug("no " + key + " exists in " + subStateKey + ":(");
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
