package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.event.ContractEventListener;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public class NoneContract implements Contract {
    @Override
    public void init(StateStore stateStore, TransactionReceiptStore txReciptStore) {
    }

    @Override
    public boolean invoke(TransactionHusk tx) {
        return true;
    }

    @Override
    public JsonObject query(JsonObject qurey) throws Exception {
        return new JsonObject();
    }

    @Override
    public void setBranchName(String branchName) {
    }

    @Override
    public void setListener(ContractEventListener listener) {
    }
}
