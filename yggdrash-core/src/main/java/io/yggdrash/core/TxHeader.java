package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Serializable;

public class TxHeader implements Serializable {

    private byte version;
    private byte[] type;
    private long timestamp;
    private byte[] from;
    private byte[] to;
    private byte[] data_hash;
    private long data_size;
    private byte[] signature;


    // Constructor

    public TxHeader() {
    }

    public TxHeader(Account from, Account to, byte[] data_hash, long data_size) throws IOException {
        this.version  = 0x00;
        this.type = new byte[7];

        makeTxHeader(from, to, data_hash, data_size);

    }


    // Method
    public void makeTxHeader(Account from, Account to, byte[] data_hash, long data_size) throws IOException {
        this.timestamp = TimeUtils.getCurrenttime();
        this.from = from.getKey().getPub_key();
        this.to = to.getKey().getPub_key();
        this.data_hash = data_hash;
        this.data_size = data_size;
        this.signature = null;

        this.signature = Signature.sign(from.getKey(), SerializeUtils.serialize(this));
    }

    public void printTxHeader() {
        System.out.println("version=" + Integer.toHexString(this.version));
        System.out.println("type=" + Hex.encodeHexString(this.type));
        System.out.println("timestamp=" + this.timestamp);
        System.out.println("from=" + Hex.encodeHexString(this.from));
        System.out.println("to=" + Hex.encodeHexString(this.to));
        System.out.println("data_hash=" + Hex.encodeHexString(this.data_hash));
        System.out.println("data_size=" + this.data_size);
        System.out.println("signature=" + Hex.encodeHexString(this.signature));
    }


}
