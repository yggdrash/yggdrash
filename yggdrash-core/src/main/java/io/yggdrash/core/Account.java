package io.yggdrash.core;

import io.yggdrash.crypto.ECKey;

public class Account {

    // <Variable>
    private ECKey key;
    private byte[] address;


    /**
     * Instantiates a new Account.
     *
     * @param key the key
     */
    public Account(ECKey key) {
        this.key = key;
        this.address = key.getAddress();
    }

    /**
     * Account Constructor.
     * - generate account with new key
     */
    public Account() {
        this.key = new ECKey();
        this.address = this.key.getAddress();
    }

    /**
     * get Account Key
     *
     * @return
     */
    public ECKey getKey() {
        return key;
    }

    /**
     * get Account Address
     *
     * @return
     */
    public byte[] getAddress() {
        return address;
    }
}
