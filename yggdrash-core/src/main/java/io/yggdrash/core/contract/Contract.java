package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.StateStore;

public interface Contract<T> {
    void init(StateStore<T> store);

    // Fix remove invoke and query method
    boolean invoke(TransactionHusk tx);

    Object query(String method, JsonObject params) throws Exception;
}
