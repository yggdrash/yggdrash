package io.yggdrash.crypto;

public class Key {

    private byte[] privateKey;
    private byte[] publicKey;

    /**
     * PKI
     * @param privateKey
     * @param publicKey
     */
    public Key(byte[] privateKey, byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * PKI
     */
    public Key() {
        this.privateKey = generateKey();
        this.publicKey = getPubKey(this.privateKey);
    }

    // <Get_set Method>
    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    // <Method>
    public byte[] generateKey() {
        // TODO key generate
        return "prikey7890123456789012".getBytes();
    }

    public byte[] getAddress() {
        return "address8901234567890".getBytes();
    }

    public byte[] getPubKey(byte[] privateKey) {
        return "pubkey7890123456789012".getBytes();
    }

}
