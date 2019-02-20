package io.yggdrash.core.store;

import com.google.gson.Gson;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.core.store.datasource.DbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionReceiptStore {
    private static final Logger log = LoggerFactory.getLogger(TransactionReceiptStore.class);


    public TransactionReceiptStore(DbSource<byte[], byte[]> source) {
        this.db = source.init();
    }

    private final DbSource<byte[], byte[]> db;
    Gson gson = new Gson();

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
