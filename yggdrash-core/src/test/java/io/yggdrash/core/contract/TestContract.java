package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.Store;
import java.math.BigInteger;

@YggdrashContract
public class TestContract implements Contract {

    @ContractStateStore
    Store<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    public void init(StateStore stateStore) {
    }

    @InvokeTransction
    public Boolean doNothing(JsonObject params) {
        // pass
        return true;
    }

    @InvokeTransction
    public TransactionReceipt transfer(JsonObject params) {

        String to = params.get("to").getAsString().toLowerCase();
        BigInteger amount = params.get("amount").getAsBigInteger();
        return txReceipt;
    }

    @ContractQuery
    public String someQuery() {
        return "";
    }

}
