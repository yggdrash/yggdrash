package io.yggdrash.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.SerializeUtils;

import java.io.Serializable;

public class Transaction implements Serializable {

    // Header
    private TransactionHeader header;

    // Data
    // TODO Data Object re modelling
    private String data;

    public Transaction() {

    }

    /**
     * Transaction Constructor
     *
     * @param header transaction header
     * @param data   transaction data
     */
    public Transaction(TransactionHeader header, String data) {
        this.data = data;
        this.header = header;
    }

    /**
     * Transaction Constructor
     *
     * @param data   transaction data(Json)
     */
    public Transaction(JsonObject data) {

        // 1. make data
        this.data = data.toString();

        // 2. make header
        byte[] bin = SerializeUtils.serialize(data);
        this.header = new TransactionHeader(HashUtil.sha3(bin), bin.length);
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
    public String getData() {
        return this.data;
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

        return header.toString() + "transactionData=" + data;
    }
}
