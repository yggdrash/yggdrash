package io.yggdrash.core;

import io.yggdrash.crypto.Key;
import io.yggdrash.util.HashUtils;

public class Account {

    // <Variable>
    private Key key;
    private byte[] stateRoot;
    private byte[] address;


    /**
     * Instantiates a new Account.
     *
     * @param key       the key
     * @param balance   the balance
     * @param stateRoot the state root
     */
    public Account(Key key, long balance, byte[] stateRoot) {
        this.key = key;
        this.stateRoot = this.stateRoot;
    }

    /**
     * Account Constructor.
     * - generate account with new key
     */
    public Account() {
        generateAccount();
    }

    public Key getKey() {
        return key;
    }

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
