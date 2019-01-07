package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import io.yggdrash.core.store.StateStore;

@YggdrashContract
public class NoneContract implements Contract {
    public void init(StateStore stateStore) {
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
