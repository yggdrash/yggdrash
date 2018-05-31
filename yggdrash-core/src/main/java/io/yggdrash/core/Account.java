package io.yggdrash.core;

import io.yggdrash.crypto.ECKey;

/**
 * Account Class
 */
public class Account {

    // <Variable>
    private ECKey key;
    private byte[] address;


    /**
     * Account Constructor
     * @param key account key
     */
    public Account(ECKey key) {
        this.key = key;
        this.address = key.getAddress();
    }

    /**
     * Account Constructor
     * - generate account with new key
     */
    public Account() {
        this.key = new ECKey();
        this.address = this.key.getAddress();
    }

    /**
     * get Account Key
     * @return
     */
    public ECKey getKey() {
        return key;
    }

    /**
     * get Account Address
     * @return address
     */
    public byte[] getAddress() {
        return address;
    }

    // <Method>


}
