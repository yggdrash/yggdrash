package io.yggdrash.contract.core.store;

import com.google.gson.JsonObject;

import java.util.Map;

public interface OutputStore {
    String put(String schemeName, String id, JsonObject jsonObject);

    void put(JsonObject blockJson);

    void put(String blockId, long index, Map<String, JsonObject> transactionMap);
}
