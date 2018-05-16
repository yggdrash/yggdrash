package io.yggdrash.core;

import io.yggdrash.crypto.Key;

public class Account {

    // <Variable>
    private Key key;
    private long balance;
    private byte[] state_root;

    // <Constructor>
    public Account(Key key, long balance, byte[] state_root) {
        this.key = key;
        this.balance = balance;
        this.state_root = state_root;
    }

    public Account() {
        generateAccount();
    }

    // <Get_set method>
    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public byte[] getState_root() {
        return state_root;
    }

    public void setState_root(byte[] state_root) {
        this.state_root = state_root;
    }

    // <Method>
    public void generateAccount() {
        this.key = new Key();
        this.balance = 0;
        this.state_root = new byte[32];
    }


}
