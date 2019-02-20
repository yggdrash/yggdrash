package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.Store;

@YggdrashContract
public class NoneContract implements Contract {

    @ContractStateStore
    Store<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    public void init(StateStore stateStore) {
    }

    @InvokeTransaction
    public boolean doNothing(JsonObject param) {
        // pass
        return true;
    }

    @ContractQuery
    public String someQuery() {
        return "";
    }
}
