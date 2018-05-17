package io.yggdrash.core;

import io.yggdrash.crypto.Key;
import io.yggdrash.util.HashUtils;

/**
 * Account Class
 */
public class Account {

    // <Variable>
    private Key key;
    private byte[] stateRoot;
    private byte[] address;


    /**
     * Account Constructor
     * @param key account key
     * @param balance account balance
     * @param state_root account state_root
     */
    public Account(Key key, long balance, byte[] state_root) {
        this.key = key;
        this.stateRoot = stateRoot;
    }

    /**
     * Account Constructor
     * - generate account with new key
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
