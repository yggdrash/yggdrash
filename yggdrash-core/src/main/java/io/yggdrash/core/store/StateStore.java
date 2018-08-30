package io.yggdrash.core.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return null;
    }

    @Override
    public boolean contains(String key) {
        return state.containsKey(key);
    }
}
