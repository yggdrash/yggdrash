package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract {
    void init(StateStore stateStore, TransactionReceiptStore txReciptStore);

    boolean invoke(TransactionHusk tx) throws Exception;

    JsonObject query(JsonObject qurey) throws Exception;
}
