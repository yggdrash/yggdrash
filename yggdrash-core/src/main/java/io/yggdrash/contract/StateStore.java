package io.yggdrash.contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore {
    private Map<String, Long> state = new ConcurrentHashMap<>();

    public StateStore() {
    }

    public Map<String, Long> getState() {
        return this.state;
    }
}
