package io.yggdrash.contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore {
    private Map<String, Long> state = new ConcurrentHashMap<>();

    public StateStore() {
        //TODO get address and value from Genesis Block
        state.put("aaa2aaab0fb041c5cb2a60a12291cbc3097352bb", 10L);
        state.put("6f19c769c78513a3a60a3618c6a11eb9a886086a", 1000000000L);
    }

    public Map<String, Long> getState() {
        return this.state;
    }
}
