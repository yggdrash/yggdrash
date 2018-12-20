package io.yggdrash.core.store;

import io.yggdrash.core.contract.TransactionReceipt;

import io.yggdrash.core.store.datasource.DbSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionReceiptStore {

    public TransactionReceiptStore(DbSource<byte[], byte[]> source) {
        this.db = source;
    }

    private final DbSource<byte[], byte[]> db;

    // TODO Remove txReceiptStore
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

    public void close() {
        this.db.close();
        txReceiptStore.clear();
    }
}
