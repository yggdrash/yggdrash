package io.yggdrash.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.SerializeUtils;

import java.io.IOException;
import java.io.Serializable;

public class Transaction implements Serializable {

    // Header
    private TransactionHeader header;

    // Data
    // TODO Data Object re modelling
    private String data;

    /**
     * Transaction Constructor
     *
     * @param header transaction header
     * @param data transaction data
     */
    public Transaction(TransactionHeader header, String data) {
        this.data = data;
        this.header = header;
    }

    /**
     * Transaction Constructor
     *
     * @param from account for creating transaction
     * @param data transaction data(Json)
     */
    @Deprecated
    public Transaction(Account from, JsonObject data) throws IOException {
        // 1. make data
        this.data = data.toString();

        // 2. make header
        byte[] bin = SerializeUtils.serialize(data);
        this.header = new TransactionHeader(from, HashUtil.sha3(bin), bin.length);
    }

    /**
     * Transaction Constructor
     *
     * @param wallet wallet for creating transaction
     * @param data transaction data(Json)
     */
    public Transaction(Wallet wallet, JsonObject data) throws IOException {

        // 1. make data
        this.data = data.toString();

        // 2. make header
        byte[] bin = SerializeUtils.serialize(data);
        this.header = new TransactionHeader(wallet, HashUtil.sha3(bin), bin.length);
    }

    /**
     * get transaction hash
     *
     * @return transaction hash
     */
    @JsonIgnore
    public String getHashString() throws IOException {
        return this.header.getHashString();
    }

    /**
     * get transaction hash
     *
     * @return transaction hash
     */
    @JsonIgnore
    public byte[] getHash() throws IOException {
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
     * @return
     */
    public TransactionHeader getHeader() {
        return header;
    }

    /**
     * print transaction
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.header.toString());
        buffer.append("transactionData=").append(this.data);

        return buffer.toString();
    }
}
