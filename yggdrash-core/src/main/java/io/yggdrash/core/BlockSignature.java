package io.yggdrash.core;

import com.google.gson.JsonObject;
import org.spongycastle.util.encoders.Hex;

public class BlockSignature implements Cloneable {

    private final byte[] signature;

    public BlockSignature(byte[] signature) {
        this.signature = signature;
    }

    public BlockSignature(JsonObject jsonObject) {
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
    }

    public BlockSignature(Wallet wallet, byte[] headerHash) {
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

    @Override
    public BlockSignature clone() throws CloneNotSupportedException {
        return (BlockSignature) super.clone();
    }

}
