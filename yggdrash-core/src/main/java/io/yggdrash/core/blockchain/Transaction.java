package io.yggdrash.core.blockchain;

import java.io.Serializable;
import java.lang.reflect.Array;

public class Transaction implements Serializable {
    String hash;
    String sign;
    long timestamp;
    Array parmas;

    public Transaction() {
        this.timestamp = System.currentTimeMillis();
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

    public Array getParmas() {
        return parmas;
    }

    public void setParmas(Array parmas) {
        this.parmas = parmas;
    }
}
