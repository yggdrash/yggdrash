package io.yggdrash.core;

import io.yggdrash.crypto.Key;

public class Account {

    // <Variable>
    private Key key;
    private long balance;
    private byte[] state_root;
    private Transactions txs;


    // <Constructor>
    public Account(Key key, long balance, byte[] state_root, Transactions txs) {
        this.key = key;
        this.balance = balance;
        this.state_root = state_root;
        this.txs = txs;
    }

    public Account() {
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

    public Transactions getTxs() {
        return txs;
    }

    public void setTxs(Transactions txs) {
        this.txs = txs;
    }


    // <Method>
    public generateAccount() {
        this.key = new Key();
        this.balance = 0;
        this.state_root = new byte[32];
        this.txs = null;
    }


}
