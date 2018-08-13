package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.Transaction;

public interface Contract {
    public boolean invoke(Transaction tx) throws Exception;
    public JsonObject query(JsonObject qurey) throws Exception;
}
