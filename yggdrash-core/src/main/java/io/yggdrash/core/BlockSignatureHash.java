package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;

public class BlockSignatureHash implements Cloneable {

    private byte[] signature;
    private byte[] headerHash;

    private ECKey.ECDSASignature ecdsaSignature;
    private ECKey ecKeyPub;

    public BlockSignatureHash(byte[] signature, byte[] headerHash) throws SignatureException {
        this.signature = signature;
        this.headerHash = headerHash;

        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        this.ecKeyPub = ECKey.signatureToKey(this.headerHash, this.ecdsaSignature);

        if (!this.ecKeyPub.verify(this.headerHash, this.ecdsaSignature)) {
            throw new SignatureException();
        }
    }

    public BlockSignatureHash(Wallet wallet, byte[] headerHash) throws SignatureException {
        this(wallet.signHashedData(headerHash), headerHash);

        if (!Arrays.equals(this.ecKeyPub.getPubKey(), wallet.getPubicKey())) {
            throw new SignatureException();
        }
    }

    public BlockSignatureHash(JsonObject jsonObject) throws SignatureException {
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
        this.headerHash = Hex.decode(jsonObject.get("headerHash").getAsString());

        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        this.ecKeyPub = ECKey.signatureToKey(this.headerHash, this.ecdsaSignature);

        if (!this.ecKeyPub.verify(this.headerHash, this.ecdsaSignature)) {
            throw new SignatureException();
        }
    }

    public long length() {
        return this.getSignature().length;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public String getSignatureHexString() {
        return Hex.toHexString(this.signature);
    }

    public byte[] getHeaderHash() {
        return this.headerHash;
    }

    public String getHeaderHashHexString() {
        return Hex.toHexString(this.headerHash);
    }

    public ECKey.ECDSASignature getEcdsaSignature() {
        return this.ecdsaSignature;
    }

    public ECKey getEcKeyPub() {
        return this.ecKeyPub;
    }

    public byte[] getPubKey() {
        return this.ecKeyPub.getPubKey();
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        jsonObject.addProperty("headerHash", Hex.toHexString(this.headerHash));

        return jsonObject;
    }

    @Override
    public BlockSignatureHash clone() throws CloneNotSupportedException {
        return (BlockSignatureHash) super.clone();
    }

}
