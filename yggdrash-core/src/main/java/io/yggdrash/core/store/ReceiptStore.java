package io.yggdrash.core.store;

import com.google.gson.Gson;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;

public class ReceiptStore {
    private final DbSource<byte[], byte[]> db;
    private final Gson gson = new Gson();

    public ReceiptStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
    }

    public void put(Receipt receipt) {
        // Receipt to ByteArray
        String receiptJson = gson.toJson(receipt);
        db.put(receipt.getTxId().getBytes(), receiptJson.getBytes());
    }

    public void put(String hash, Receipt receipt) {
        String receiptJson = gson.toJson(receipt);
        db.put(hash.getBytes(), receiptJson.getBytes());
    }

    public Receipt get(String key) {
        byte[] receipt = db.get(key.getBytes());
        if (receipt != null) {
            // Receipt from ByteArray
            String receiptJson = new String(receipt);
            return gson.fromJson(receiptJson, ReceiptImpl.class);
        }
        return new ReceiptImpl();
    }

    // key -> txHash or blockHash + index
    public boolean contains(String key) {
        return db.get(key.getBytes()) != null;
    }

    public void close() {
        this.db.close();
    }
}
