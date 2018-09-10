package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.contract.Contract;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public class Runtime<T> {

    private final StateStore<T> stateStore;
    private final TransactionReceiptStore txReceiptStore;

    public Runtime(StateStore<T> stateStore, TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.txReceiptStore = txReceiptStore;
    }

    public boolean invoke(Contract contract, TransactionHusk tx) throws Exception {
        contract.init(stateStore, txReceiptStore);
        return contract.invoke(tx);
    }

    public JsonObject query(Contract contract, JsonObject query) throws Exception {
        contract.init(stateStore, txReceiptStore);
        return contract.query(query);
    }

    public StateStore<T> getStateStore() {
        return this.stateStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.txReceiptStore;
    }
}