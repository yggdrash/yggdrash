package io.yggdrash.crypto;

public class Key {

    // <Variable>
    private byte[] pri_key;
    private byte[] pub_key;

    // <Constructor>
    public Key(byte[] pri_key, byte[] pub_key) {
        this.pri_key = pri_key;
        this.pub_key = pub_key;
    }

    public Key() {
        this.pri_key = generateKey();
        this.pub_key = getPubKey(this.pri_key);
    }

    // <Get_set Method>
    public byte[] getPri_key() {
        return pri_key;
    }

    public void setPri_key(byte[] pri_key) {
        this.pri_key = pri_key;
    }

    public byte[] getPub_key() {
        return pub_key;
    }

    public void setPub_key(byte[] pub_key) {
        this.pub_key = pub_key;
    }


    // <Method>
    public byte[] generateKey() {
        return "prikey7890123456789012".getBytes();
    }

    public byte[] getAddress() {
        return "address8901234567890".getBytes();
    }

    public byte[] getPubKey(byte[] pri_key) {
        return "pubkey7890123456789012".getBytes();
    }

}
