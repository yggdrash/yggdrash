package io.yggdrash.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.genesis.TransactionInfo;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;

public class Transaction implements Cloneable {

    // Transaction Data Format v0.0.3
    private TransactionHeader header;
    private byte[] signature;
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
        this(header, signature.getSignature(), body);
    }

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param signature transaction signature
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header,
                       byte[] signature, TransactionBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
        verify();
    }

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param wallet wallet for signning
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, Wallet wallet, TransactionBody body) {
        this(header, wallet.signHashedData(header.getHashForSignning()), body);
    }

    public Transaction(JsonObject jsonObject) {
        this(new TransactionHeader(jsonObject.get("header").getAsJsonObject()),
                Hex.decode(jsonObject.get("signature").getAsString()),
                new TransactionBody(jsonObject.getAsJsonArray("body")));
    }

    public Transaction(byte[] txBytes) {
        int position = 0;

        byte[] headerBytes = new byte[84];
        System.arraycopy(txBytes, 0, headerBytes, 0, headerBytes.length);
        this.header = new TransactionHeader(headerBytes);
        position += headerBytes.length;

        byte[] sigBytes = new byte[65];
        System.arraycopy(txBytes, position, sigBytes, 0, sigBytes.length);
        position += sigBytes.length;
        this.signature = sigBytes;

        byte[] bodyBytes = new byte[txBytes.length - headerBytes.length - sigBytes.length];
        System.arraycopy(txBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;
        this.body = new TransactionBody(bodyBytes);

        if (position != txBytes.length) {
            throw new NotValidateException();
        }
        verify();
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
     * Get Transaction signature.
     *
     * @return transaction signature
     */
    public byte[] getSignature() {
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
        bao.write(this.signature);

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
     * Get the public key.
     *
     * @return public key
     */
    public byte[] getPubKey() throws SignatureException {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        ECKey ecKeyPub = ECKey.signatureToKey(this.header.getHashForSignning(), ecdsaSignature);

        return ecKeyPub.getPubKey();
    }

    /**
     * Get the public key as HexString.
     *
     * @return the public key as HexString
     */
    public String getPubKeyHexString() throws IOException, SignatureException {
        return Hex.toHexString(this.getPubKey());
    }

    /**
     * Get the address as binary.
     *
     * @return address
     */
    public byte[] getAddress() throws IOException, SignatureException {

        byte[] pubKey = this.getPubKey();
        return HashUtil.sha3omit12(
                Arrays.copyOfRange(pubKey, 1, pubKey.length));
    }

    /**
     * Get the address as HexString.
     *
     * @return address as HexString
     */
    public String getAddressToString() throws IOException, SignatureException {
        return Hex.toHexString(this.getAddress());
    }

    /**
     * Get the Transaction length (Header + Signature + Body).
     *
     * @return tx length
     */
    public long length() throws IOException {
        return this.header.length() + this.signature.length + this.body.length();
    }

    public boolean verify() {

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        byte[] hashedHeader = this.header.getHashForSignning();
        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);
    }

    /**
     * Convert from Transaction.class to JsonObject.
     *
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
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
        bao.write(this.signature);
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

    public static Proto.Transaction toProtoTransaction(Transaction tx) {

        Proto.Transaction.Header protoHeader;
        protoHeader = Proto.Transaction.Header.newBuilder()
            .setChain(ByteString.copyFrom(tx.getHeader().getChain()))
            .setVersion(ByteString.copyFrom(tx.getHeader().getVersion()))
            .setType(ByteString.copyFrom(tx.getHeader().getType()))
            .setTimestamp(ByteString.copyFrom(
                    ByteUtil.longToBytes(tx.getHeader().getTimestamp())))
            .setBodyHash(ByteString.copyFrom(tx.getHeader().getBodyHash()))
            .setBodyLength(ByteString.copyFrom(
                    ByteUtil.longToBytes(tx.getHeader().getBodyLength())))
            .build();

        Proto.Transaction protoTransaction = Proto.Transaction.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(tx.getSignature()))
                .setBody(ByteString.copyFrom(tx.getBody().toBinary()))
                .build();

        return protoTransaction;
    }

    public static Transaction toTransaction(Proto.Transaction protoTransaction) {

        TransactionHeader txHeader = new TransactionHeader(
                protoTransaction.getHeader().getChain().toByteArray(),
                protoTransaction.getHeader().getVersion().toByteArray(),
                protoTransaction.getHeader().getType().toByteArray(),
                ByteUtil.byteArrayToLong(
                        protoTransaction.getHeader().getTimestamp().toByteArray()),
                protoTransaction.getHeader().getBodyHash().toByteArray(),
                ByteUtil.byteArrayToLong(
                        protoTransaction.getHeader().getBodyLength().toByteArray())
                );

        TransactionSignature txSignature =  new TransactionSignature(
                protoTransaction.getSignature().toByteArray()
        );

        TransactionBody txBody = new TransactionBody(
                protoTransaction.getBody().toStringUtf8()
        );

        return new Transaction(txHeader, txSignature, txBody);

    }

    public static Transaction fromTransactionInfo(TransactionInfo txi) {
        TransactionHeader txHeader = new TransactionHeader(
                Hex.decode(txi.header.chain),
                Hex.decode(txi.header.version),
                Hex.decode(txi.header.type),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.timestamp)),
                Hex.decode(txi.header.bodyHash),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.bodyLength))
        );

        TransactionBody txBody = new TransactionBody(new Gson().toJson(txi.body));

        return new Transaction(txHeader, Hex.decode(txi.signature), txBody);
    }

}
