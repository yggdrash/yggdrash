package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

public class ContractQry {

    public static JsonObject createQuery(String branchId, String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", branchId);
        query.addProperty("method", method);
        query.add("params", params);

        return query;
    }

    public static JsonArray createParams(String key, String value) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty(key, value);
        params.add(param);

        return params;
    }

    public static JsonArray createParams(String key1, String value1, String key2, String value2) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty(key1, value1);
        param.addProperty(key2, value2);
        params.add(param);

        return params;
    }

    public static JsonArray createParams(Map<String, String> data) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();

        for (String key : data.keySet()) {
            param.addProperty(key, data.get(key));
        }

        params.add(param);

        return params;
    }
}
