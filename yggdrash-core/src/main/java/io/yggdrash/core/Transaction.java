package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Serializable;

public class Transaction implements Serializable {

    // <Variable>
    private TxHeader header;
    private JsonObject data;


    // Constructor
    public Transaction() {

    }

    /**
     * Transaction Constructor
     * @param from account for creating transaction
     * @param to account for targeting
     * @param data transaction data(Json)
     */
    public Transaction(Account from, Account to, JsonObject data) {
        makeTransaction(from, to, data);
    }

    // generate TX for testing

    /**
     * Transaction Constructor with tx data
     * @param data tx data
     */
    public Transaction(String data) {
        JsonObject data1 = new JsonObject();
        data1.addProperty("key", "balance");
        data1.addProperty("operator", "transfer");
        data1.addProperty("value", data);
        makeTransaction(new Account(), new Account(), data1);
    }

    private void makeTransaction(Account from, Account to, JsonObject data) {

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

    /**
     * get transaction hash
     * @return transaction hash
     */
    public String getHashString() {
        return this.header.hashString();
    }

    /**
     * get transaction hash
     * @return transaction hash
     */
    public byte[] getHash() {
        return this.header.hash();
    }

    /**
     * get account for created tx
     * @return transaction hash
     */
    public String getFrom() {
        return Hex.encodeHexString(header.getFrom());
    }

    /**
     * get transaction data
     * @return tx data
     */
    public String getData() {
        return this.data.toString();
    }

    /**
     * print transaction
     */
    public void printTransaction() {
        this.header.printTxHeader();
        System.out.println("TX="+this.data.toString());
    }

}
