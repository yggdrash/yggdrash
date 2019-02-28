package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import io.yggdrash.common.store.StateStore;

@YggdrashContract
public class NoneContract implements Contract {

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;


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
