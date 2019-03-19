package io.yggdrash.core.store;

import com.google.gson.Gson;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;

public class TransactionReceiptStore {
    private final DbSource<byte[], byte[]> db;
    private final Gson gson = new Gson();

    public TransactionReceiptStore(DbSource<byte[], byte[]> source) {
        this.db = source.init();
    }

    public void put(TransactionReceipt txReceipt) {
        // TransactionReceipt to ByteArray
        String txReceiptJson = gson.toJson(txReceipt);
        db.put(txReceipt.getTxId().getBytes(), txReceiptJson.getBytes());
    }

    public TransactionReceipt get(String txHash) {
        byte[] transactionReceipt = db.get(txHash.getBytes());
        // TransactionReceipt from ByteArray
        String txReceiptJson = new String(transactionReceipt);
        return gson.fromJson(txReceiptJson, TransactionReceiptImpl.class);
    }

    public void close() {
        this.db.close();
    }
}
