package io.yggdrash.core.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore<V> implements Store<String, V> {

    private static final Logger logger = LoggerFactory.getLogger(StateStore.class);
    private final Map<String, V> state;

    public StateStore() {
        this.state = new ConcurrentHashMap<>();
    }

    public Map<String, V> getState() {
        return this.state;
    }

    public void replace(String key, V value) {
        state.replace(key, value);
    }

    @Override
    public void put(String key, V value) {
        state.put(key, value);
    }

    @Override
    public V get(String key) {
        return state.get(key);
    }

    @Override
    public Set<V> getAll() {
        Set<V> res = new HashSet<>();
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
