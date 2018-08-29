package io.yggdrash.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.HashUtil;

public class Transaction {

    private TransactionHeader header;
    private TransactionSignature signature;
    private TransactionBody body;

    public Transaction() {
    }

    /**
     * Transaction Constructor
     *
     * @param header transaction header
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, TransactionBody body) {
        this.header = header;
        this.body = body;
    }

    /**
     * Transaction Constructor
     *
     * @param data   transaction data(Json)
     */
    public Transaction(Wallet wallet, TransactionBody body) {

        // 1. make data
        this.body = body;

        // 2. make header
        byte[] bin = null; // for test ; SerializeUtils.serialize(data);
        this.header = new TransactionHeader(wallet, HashUtil.sha3(bin), bin.length);
    }

    /**
     * get transaction hash
     *
     * @return transaction hash
     */
    @JsonIgnore
    public String getHashString() {
        return this.header.getHashString();
    }

    /**
     * get transaction hash
     *
     * @return transaction hash
     */
    @JsonIgnore
    public byte[] getHash() {
        return this.header.getHash();
    }

    /**
     * get transaction data
     *
     * @return tx data
     */
    public String getBody() {
//        return this.body;
        return null;
    }

    /**
     * get Transaction Header
     *
     * @return tx header
     */
    public TransactionHeader getHeader() {
        return header;
    }

    /**
     * print transaction
     */
    public String toString() {
        return header.toString() + "transactionBody=" + body;
    }

    /**
     * Convert from Transaction.class to JSON string.
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {
        //todo: change to serialize method

        JsonObject jsonObject = this.getHeader().toJsonObject();
//        jsonObject.add("data", new Gson().fromJson(this.body, JsonObject.class)); // for test

        return jsonObject;
    }

}
