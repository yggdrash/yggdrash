package io.yggdrash.core;

import com.google.gson.JsonObject;

public class TxHeader {

    private byte version;
    private byte[] type;
    private long timestamp = 0;
    private byte[] from = new byte[32];
    private byte[] to = new byte[32];
    private byte[] data_hash = new byte[32];
    private long data_size = 0;
    private byte[] signature = new byte[32];


    // Constructor

    public TxHeader() {
        this.version  = 0x00;
        this.type = {0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    }


    // Method
    public boolean makeTxHeader(Account acc_from, Account acc_to, JsonObject data) {
        this.timestamp = io.yggdrash.util.time.getTimestamp();
        this.from = acc_from.getHeader().getPub_key();
        this.to = acc_to.getHeader().getPub_key();
        this.data_size = data.size();
        this.data_hash = data.getHash();
        this.signature = io.yggdrash.util.sign.getSignature();

    }

}
