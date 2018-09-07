package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;

public class TransactionSignatureHash implements Cloneable {

//    private byte[] signature;
//    private byte[] headerHash;
//
//    private ECKey.ECDSASignature ecdsaSignature;
//    private ECKey ecKeyPub;
//
//    public TransactionSignatureHash(byte[] signature, byte[] headerHash) throws SignatureException {
//        this.signature = signature;
//        this.headerHash = headerHash;
//
//        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
//        this.ecKeyPub = ECKey.signatureToKey(this.headerHash, this.ecdsaSignature);
//
//        if (!this.ecKeyPub.verify(this.headerHash, this.ecdsaSignature)) {
//            throw new SignatureException();
//        }
//    }
//
//    public TransactionSignatureHash(Wallet wallet, byte[] headerHash) throws SignatureException {
//        this(wallet.signHashedData(headerHash), headerHash);
//
//        if (!Arrays.equals(this.ecKeyPub.getPubKey(), wallet.getPubicKey())) {
//            throw new SignatureException();
//        }
//    }
//
//    public TransactionSignatureHash(JsonObject jsonObject) throws SignatureException {
//        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
//        this.headerHash = Hex.decode(jsonObject.get("headerHash").getAsString());
//
//        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
//        this.ecKeyPub = ECKey.signatureToKey(this.headerHash, this.ecdsaSignature);
//
//        if (!this.ecKeyPub.verify(this.headerHash, this.ecdsaSignature)) {
//            throw new SignatureException();
//        }
//    }
//
//    public long length() {
//        return this.getSignature().length;
//    }
//
//    public byte[] getSignature() {
//        return this.signature;
//    }
//
//    public String getSignatureHexString() {
//        return Hex.toHexString(this.signature);
//    }
//
//    public byte[] getHeaderHash() {
//        return this.headerHash;
//    }
//
//    public String getHeaderHashHexString() {
//        return Hex.toHexString(this.headerHash);
//    }
//
//    private ECKey.ECDSASignature getEcdsaSignature() {
//        return this.ecdsaSignature;
//    }
//
//    private ECKey getEcKeyPub() {
//        return this.ecKeyPub;
//    }
//
//    public byte[] getPubKey() {
//        return this.ecKeyPub.getPubKey();
//    }
//
//    public boolean verify() {
//        return this.getEcKeyPub().verify(this.headerHash, this.ecdsaSignature);
//    }
//
//    public JsonObject toJsonObject() {
//        JsonObject jsonObject = new JsonObject();
//
//        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
//        jsonObject.addProperty("headerHash", Hex.toHexString(this.headerHash));
//
//        return jsonObject;
//    }
//
//    public String toString() {
//        return this.toJsonObject().toString();
//    }
//
//    @Override
//    public TransactionSignatureHash clone() throws CloneNotSupportedException {
//        return (TransactionSignatureHash) super.clone();
//    }

}
