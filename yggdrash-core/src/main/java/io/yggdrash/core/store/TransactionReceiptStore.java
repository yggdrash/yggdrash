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
        // TransctionReceipt to Bytearray
        String txReceiptJson = gson.toJson(txReceipt);
        db.put(txReceipt.getTxId().getBytes(), txReceiptJson.getBytes());
    }

    public TransactionReceipt get(String txHash) {
        byte[] tranasctionReceipt = db.get(txHash.getBytes());
        // TransctionReceipt from ByteArray
        String txReceiptJson = new String(tranasctionReceipt);
        return gson.fromJson(txReceiptJson, TransactionReceiptImpl.class);
    }

    public void close() {
        this.db.close();
    }
}
