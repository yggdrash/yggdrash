package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.event.ContractEventListener;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract<T> {
    void init(StateStore<T> store, TransactionReceiptStore txReceiptStore);

    boolean invoke(TransactionHusk tx) throws Exception;

    JsonObject query(JsonObject query) throws Exception;

    void setBranchName(String branchName);

    void setListener(ContractEventListener listener);
}
