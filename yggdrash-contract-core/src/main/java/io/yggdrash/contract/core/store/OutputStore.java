package io.yggdrash.contract.core.store;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

public interface OutputStore {
    String put(String schemeName, String id, JsonObject jsonObject);

    String put(JsonObject blockJson);

    Set<String> put(long blockNo, Map<String, JsonObject> transactionMap);
}
