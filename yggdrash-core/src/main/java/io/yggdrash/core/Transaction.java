package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.TxHeader;

import java.io.Serializable;

public class Transaction implements Serializable {

    // Header
    private TxHeader header;

    // Data
    private JsonObject data;


    // Constructor
    public Transaction() {
        this.header = new TxHeader();
        this.data = new JsonObject();
    }

    public boolean makeTransaction(TxHeader header, JsonObject data) {

        // 1. make header
        this.header = new TxHeader();
        if(!this.header.makeTxHeader())
            return false;

        // 2. make data
        if(!makeTxData(this.data))
            return false;

        return ture;
    }


    public boolean makeTxData() {

    }


}
