package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public class NoneContract implements Contract {
    @Override
    public void init(StateStore stateStore, TransactionReceiptStore txReceiptStore) {
    }

    @Override
    public boolean invoke(TransactionHusk tx) {
        return true;
    }

    @Override
    public JsonObject query(JsonObject query) {
        return new JsonObject();
    }
}
