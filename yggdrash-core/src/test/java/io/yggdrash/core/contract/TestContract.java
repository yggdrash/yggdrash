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

import java.math.BigInteger;

@YggdrashContract
public class TestContract implements Contract {

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    public void init(StateStore stateStore) {
    }

    @InvokeTransaction
    public Boolean doNothing(JsonObject params) {
        // pass
        return true;
    }

    @InvokeTransaction
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
