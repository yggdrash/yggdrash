package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract<T> {
    void init(StateStore<T> store, TransactionReceiptStore txReceiptStore);

    // Fix remove invoke and query method
    boolean invoke(TransactionHusk tx);

    Object query(String method, JsonObject params) throws Exception;
}
