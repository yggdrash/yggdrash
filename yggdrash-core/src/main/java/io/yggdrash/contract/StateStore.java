package io.yggdrash.contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore {
    private Map<String, Long> state = new ConcurrentHashMap<>();

    public StateStore() {
        state.put("aaa2aaab0fb041c5cb2a60a12291cbc3097352bb", 10000L);
    }

    public Map<String, Long> getState() {
        return this.state;
    }
}
