package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract<V> {
    void init(StateStore<V> store, TransactionReceiptStore txReceiptStore);

    boolean invoke(TransactionHusk tx) throws Exception;

    JsonObject query(JsonObject query) throws Exception;
}
