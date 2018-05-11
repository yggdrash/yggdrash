package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;

import java.io.IOException;
import java.io.Serializable;

public class Transaction implements Serializable {

    // Header
    private TxHeader header;

    // Data
    private JsonObject data;


    // Constructor
    public Transaction(Account from, Account to, JsonObject data) throws IOException {
        makeTransaction(from, to, data);
    }

    public void makeTransaction(Account from, Account to, JsonObject data) throws IOException {

        // 1. make data
        this.data = data;

        // 2. make header
        byte[] bin = SerializeUtils.serialize(data);
        this.header = new TxHeader(from, to, HashUtils.sha256(bin), bin.length);

    }

    public String getHashString() {
        return this.header.hashString();
    }

    public byte[] getHash() {
        return this.header.hash();
    }

    public void printTransaction() {
        this.header.printTxHeader();
        System.out.println("TX="+this.data.toString());
    }

}
