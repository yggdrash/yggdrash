package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.StateStore;

public class NoneContract implements Contract {
    public void init(StateStore stateStore) {
    }

    @Override
    public boolean invoke(TransactionHusk tx) {
        return true;
    }

    @Override
    public Object query(String method, JsonObject params) throws Exception {
        return null;
    }

    @InvokeTransction
    public boolean doNothing(JsonObject param) {
        // pass
        return true;
    }

    @ContractQuery
    public String someQuery() {
        return "";
    }
}
