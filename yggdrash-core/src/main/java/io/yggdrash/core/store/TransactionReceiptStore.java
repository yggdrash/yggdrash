package io.yggdrash.core.store;

import com.google.gson.Gson;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.core.store.datasource.DbSource;

public class TransactionReceiptStore {

    public TransactionReceiptStore(DbSource<byte[], byte[]> source) {
        this.db = source;
    }

    private final DbSource<byte[], byte[]> db;
    Gson gson = new Gson();

    public void put(String txHash, TransactionReceipt txReceipt) {
        String txReceiptJson = gson.toJson(txReceipt);
        db.put(txHash.getBytes(), txReceiptJson.getBytes());
    }

    public TransactionReceipt get(String txHash) {
        byte[] tranasctionReceipt = db.get(txHash.getBytes());
        String txReceiptJson = new String(tranasctionReceipt);
        TransactionReceiptImpl txReceipt = gson.fromJson(txReceiptJson, TransactionReceiptImpl.class);

        return txReceipt;
    }

    public void close() {
        this.db.close();
    }
}
