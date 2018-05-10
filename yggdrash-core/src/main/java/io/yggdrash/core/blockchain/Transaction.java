package io.yggdrash.core.blockchain;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction
 */
public class Transaction implements Serializable {
    String hash;
    String sign;
    long timestamp;
    List params;

    public Transaction() {
        this.timestamp = System.currentTimeMillis();
    }
    // new Transaction from Tranaction
    public Transaction(Transaction tx) {
        this.hash = tx.hash;
        this.sign = tx.hash;
        this.timestamp = tx.timestamp;
    }

    public boolean validation() {
        return true;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Object[] getParams() {
        return this.params.toArray();
    }

    public void setParams(Object[] params) {
        this.params.addAll(Arrays.asList(params));
    }

    public boolean validate() {
        // Make Validate
        return true;
    }

}
