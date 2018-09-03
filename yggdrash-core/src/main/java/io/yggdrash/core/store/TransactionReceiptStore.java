package io.yggdrash.core.store;

import io.yggdrash.core.TransactionReceipt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionReceiptStore {

    private final Map<String, TransactionReceipt> txReceiptStore = new ConcurrentHashMap<>();

    public void put(String txHash, TransactionReceipt txReceipt) {
        txReceiptStore.put(txHash, txReceipt);
    }

    public TransactionReceipt get(String txHash) {
        return txReceiptStore.get(txHash);
    }

    public Map<String, TransactionReceipt> getTxReceiptStore() {
        return txReceiptStore;
    }
}
