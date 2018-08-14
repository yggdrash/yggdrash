package io.yggdrash.contract;

import java.util.HashMap;

public class StateStore {
    private HashMap<String, Integer> state = new HashMap<>();

    public StateStore() {
        //TODO get address and value from Genesis Block
        state.put("aaa2aaab0fb041c5cb2a60a12291cbc3097352bb", 10);
    }

    public HashMap<String, Integer> getState() {
        return this.state;
    }
}
