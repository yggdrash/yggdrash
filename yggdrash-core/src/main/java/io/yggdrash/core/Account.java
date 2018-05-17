package io.yggdrash.core;

import io.yggdrash.crypto.Key;
import io.yggdrash.util.HashUtils;

public class Account {

    // <Variable>
    private Key key;
    private byte[] stateRoot;
    private byte[] address;

    /**
     * Account Model
     * @param key   Account Key
     * @param stateRoot Account StateRoot
     */
    public Account(Key key, byte[] stateRoot) {
        this.key = key;
        this.stateRoot = stateRoot;
    }

    /**
     * Account Model
     */
    public Account() {
        generateAccount();
    }

    /**
     * get Account Key
     * @return
     */
    public Key getKey() {
        return key;
    }

    /**
     * set Account Key
     * @param key
     */
    public void setKey(Key key) {
        this.key = key;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    // <Method>
    public void generateAccount() {
        this.key = new Key();
        this.stateRoot = new byte[32];
    }

    public void generateAddress() {
        this.address = HashUtils.sha256(this.getKey().getPublicKey());
    }
}
