package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;

public class TransactionSignature implements Cloneable {

    private byte[] signature;

    public TransactionSignature(byte[] signature) {
        this.signature = signature;
    }

    public TransactionSignature(JsonObject jsonObject) {
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
    }

    public TransactionSignature(Wallet wallet, byte[] headerHash) {
        this(wallet.signHashedData(headerHash));
    }

    public long length() {
        return this.signature.length;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public String getSignatureHexString() {
        return Hex.toHexString(this.signature);
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("signature", Hex.toHexString(this.signature));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    @Override
    public TransactionSignature clone() throws CloneNotSupportedException {
        return (TransactionSignature) super.clone();
    }

}
