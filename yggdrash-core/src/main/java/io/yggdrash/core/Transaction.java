package io.yggdrash.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;

public class Transaction implements Cloneable {

    // Transaction Data Format v0.0.3
    private TransactionHeader header;
    private TransactionSignature signature;
    private TransactionBody body;

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param signature transaction signature
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, TransactionSignature signature, TransactionBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param wallet wallet for signning
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, Wallet wallet, TransactionBody body) throws SignatureException, IOException {

        this.body = body;
        this.header = header;
        this.header.setTimestamp(TimeUtils.time());
        this.signature = new TransactionSignature(wallet, this.header.getHeaderHashForSigning());
    }

    /**
     * Get TransactionHeader.
     *
     * @return transaction header class
     */
    public TransactionHeader getHeader() {
        return this.header;
    }

    /**
     * Get TransactionBody.
     *
     * @return transaction body class
     */
    public TransactionBody getBody() {
        return this.body;
    }

    /**
     * Get TransactionSignature.
     *
     * @return transaction signature class
     */
    public TransactionSignature getSignature() {
        return signature;
    }

    /**
     * Convert from Transaction.class to JsonObject.
     *
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.addProperty("signature", this.signature.getSignatureHexString());
        jsonObject.add("body", this.body.getBody());

        return jsonObject;
    }

    /**
     * Get transaction hash. SHA3(header | signature)
     *
     * @return transaction hash
     */
    public byte[] getHash() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature.getSignature());

        return HashUtil.sha3(bao.toByteArray());
    }

    /**
     * Get transaction hash(HexString).
     *
     * @return transaction hash(HexString)
     */
    public String getHashString() throws IOException {
        return Hex.toHexString(this.getHash());
    }

    /**
     * Print transaction.
     */
    public String toString() {
        return this.toJsonObject().toString();
    }

    /**
     * Print transaction to pretty JsonObject.
     */
    public String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

    /**
     * Get ECKey (include pubKey).
     *
     * @return ECKey(include pubKey)
     */
    public ECKey getEcKey() {
        return this.signature.getEcKeyPub();
    }

    /**
     * Get the public key.
     *
     * @return public key
     */
    public byte[] getPubKey() {
        return this.getEcKey().getPubKey();
    }

    /**
     * Get the public key as HexString.
     *
     * @return the public key as HexString
     */
    public String getPubKeyHexString() {
        return Hex.toHexString(this.getEcKey().getPubKey());
    }

    /**
     * Get the address as binary.
     *
     * @return address
     */
    public byte[] getAddress() {
        return this.getEcKey().getAddress();
    }

    /**
     * Get the address as HexString.
     *
     * @return address as HexString
     */
    public String getAddressToString() {
        return Hex.toHexString(getAddress());
    }

    /**
     * Get the Transaction length (Header + Signature + Body).
     *
     * @return tx length
     */
    public long length() {
        return this.header.length() + this.signature.length() + this.body.length();
    }


    @Override
    public Transaction clone() throws CloneNotSupportedException {
        Transaction tx = (Transaction) super.clone();
        tx.header = this.header.clone();
        tx.signature = this.signature.clone();
        tx.body = this.body.clone();

        return tx;
    }



}
