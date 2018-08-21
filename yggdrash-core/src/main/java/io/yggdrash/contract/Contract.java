package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract {
    public void init(StateStore stateStore, TransactionReceiptStore txReciptStore);

    public boolean invoke(TransactionHusk tx) throws Exception;

    public JsonObject query(JsonObject qurey) throws Exception;
}
