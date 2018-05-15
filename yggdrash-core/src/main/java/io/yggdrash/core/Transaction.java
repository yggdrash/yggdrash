package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Serializable;

public class Transaction implements Serializable {

    // Header
    private TxHeader header;

    // Data
    private JsonObject data;


    // Constructor
    public Transaction() {

    }

    public Transaction(Account from, Account to, JsonObject data) throws IOException {
        makeTransaction(from, to, data);
    }

    // generate TX for testing
    public Transaction(String data) {
        JsonObject data1 = new JsonObject();
        data1.addProperty("key", "balance");
        data1.addProperty("operator", "transfer");
        data1.addProperty("value", data);
        makeTransaction(new Account(), new Account(), data1);
    }

    public void makeTransaction(Account from, Account to, JsonObject data) {

        // 1. make data
        this.data = data;

        // 2. make header
        try {
            byte[] bin = SerializeUtils.serialize(data);
            this.header = new TxHeader(from, to, HashUtils.sha256(bin), bin.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getHashString() {
        return this.header.hashString();
    }

    public byte[] getHash() {
        return this.header.hash();
    }

    public String getFrom() {
        return Hex.encodeHexString(header.getFrom());
    }

    public String getData() {
        return this.data.toString();
    }

    public void printTransaction() {
        this.header.printTxHeader();
        System.out.println("TX="+this.data.toString());
    }

}
