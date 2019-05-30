package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.store.ReadWriterStore;

public class StoreAdapter implements ReadWriterStore<String, JsonObject> {
    private final ReadWriterStore<String, JsonObject> stateStore;
    private final String nameSpace;

    public StoreAdapter(ReadWriterStore<String, JsonObject> stateStore, String nameSpace) {
        this.stateStore = stateStore;
        this.nameSpace = nameSpace;
    }

    private String getNameSpaceKey(String key) {
        return String.format("%s%s",nameSpace,key);
    }

    @Override
    public void put(String key, JsonObject value) {
        this.stateStore.put(getNameSpaceKey(key), value);
    }

    @Override
    public boolean contains(String key) {
        return this.stateStore.contains(getNameSpaceKey(key));
    }

    @Override
    public void close() {
        this.stateStore.close();
    }

    @Override
    public JsonObject get(String key) {
        return this.stateStore.get(getNameSpaceKey(key));
    }
}
