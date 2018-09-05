package io.yggdrash.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
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
    public Transaction(TransactionHeader header,
                       TransactionSignature signature, TransactionBody body) {
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
    public Transaction(TransactionHeader header, Wallet wallet, TransactionBody body)
            throws SignatureException, IOException {

        this.body = body;
        this.header = header;
        this.header.setTimestamp(TimeUtils.time());
        this.signature = new TransactionSignature(wallet, this.header.getHashForSignning());
    }

    public Transaction(JsonObject jsonObject) throws SignatureException {

        this.header = new TransactionHeader(jsonObject.get("header").getAsJsonObject());
        this.signature = new TransactionSignature(jsonObject.get("signature").getAsJsonObject());
        this.body = new TransactionBody(jsonObject.getAsJsonArray("body"));

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
     * Get ECKey (include pubKey).
     *
     * @return ECKey(include pubKey)
     */
    public ECKey getEcKeyPub() {
        return this.signature.getEcKeyPub();
    }

    /**
     * Get the public key.
     *
     * @return public key
     */
    public byte[] getPubKey() {
        return this.getEcKeyPub().getPubKey();
    }

    /**
     * Get the public key as HexString.
     *
     * @return the public key as HexString
     */
    public String getPubKeyHexString() {
        return Hex.toHexString(this.getEcKeyPub().getPubKey());
    }

    /**
     * Get the address as binary.
     *
     * @return address
     */
    public byte[] getAddress() {
        return this.getEcKeyPub().getAddress();
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
    public long length() throws IOException {
        return this.header.length() + this.signature.length() + this.body.length();
    }

    /**
     * Convert from Transaction.class to JsonObject.
     *
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.add("signature", this.signature.toJsonObject());
        jsonObject.add("body", this.body.getBody());

        return jsonObject;
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

    public byte[] toBinary() throws IOException {

        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature.getSignature());
        bao.write(this.body.toBinary());

        return bao.toByteArray();
    }

    @Override
    public Transaction clone() throws CloneNotSupportedException {
        Transaction tx = (Transaction) super.clone();
        tx.header = this.header.clone();
        tx.signature = this.signature.clone();
        tx.body = this.body.clone();

        return tx;
    }

    public Proto.Transaction toProtoTransaction() {
        return this.toProtoTransaction(this);
    }

    public static Proto.Transaction toProtoTransaction(Transaction tx) {
        Proto.Transaction.Header protoHeader = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(tx.getHeader().getChain()))
                .setVersion(ByteString.copyFrom(tx.getHeader().getVersion()))
                .setType(ByteString.copyFrom(tx.getHeader().getType()))
                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(tx.getHeader().getTimestamp())))
                .setBodyHash(ByteString.copyFrom(tx.getHeader().getBodyHash()))
                .setBodyLength(ByteString.copyFrom(ByteUtil.longToBytes(tx.getHeader().getBodyLength())))
                .build();

        Proto.Transaction protoTransaction = Proto.Transaction.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(tx.getSignature().getSignature()))
                .setBody(ByteString.copyFrom(tx.getBody().toBinary()))
                .build();

        return protoTransaction;
    }

    public static Transaction toTransaction(Proto.Transaction protoTransaction) throws SignatureException, IOException {

        TransactionHeader txHeader = new TransactionHeader(
                protoTransaction.getHeader().getChain().toByteArray(),
                protoTransaction.getHeader().getVersion().toByteArray(),
                protoTransaction.getHeader().getType().toByteArray(),
                ByteUtil.byteArrayToLong(protoTransaction.getHeader().getTimestamp().toByteArray()),
                protoTransaction.getHeader().getBodyHash().toByteArray(),
                ByteUtil.byteArrayToLong(protoTransaction.getHeader().getBodyLength().toByteArray())
                );

        TransactionSignature txSignature =  new TransactionSignature(
                protoTransaction.getSignature().toByteArray(),
                txHeader.getHashForSignning()
        );

        TransactionBody txBody = new TransactionBody(
                protoTransaction.getBody().toStringUtf8()
        );

        return new Transaction(txHeader, txSignature, txBody);

    }

}
