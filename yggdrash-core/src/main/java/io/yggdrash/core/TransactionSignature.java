package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;

public class TransactionSignature implements Cloneable {

    private byte[] signature;
    private byte[] bodyHash;

    private ECKey.ECDSASignature ecdsaSignature;
    private ECKey ecKeyPub;

    public TransactionSignature(byte[] signature, byte[] bodyHash) throws SignatureException {
        this.signature = signature;
        this.bodyHash = bodyHash;

        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        this.ecKeyPub = ECKey.signatureToKey(this.bodyHash, this.ecdsaSignature);

        if (!this.ecKeyPub.verify(this.bodyHash, this.ecdsaSignature)) {
            throw new SignatureException();
        }
    }

    public TransactionSignature(Wallet wallet, byte[] bodyHash) throws SignatureException {
        this(wallet.signHashedData(bodyHash), bodyHash);

        if (!Arrays.equals(this.ecKeyPub.getPubKey(), wallet.getPubicKey())) {
            throw new SignatureException();
        }
    }

    public TransactionSignature(JsonObject jsonObject) throws SignatureException {
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
        this.bodyHash = Hex.decode(jsonObject.get("bodyHash").getAsString());

        this.ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        this.ecKeyPub = ECKey.signatureToKey(this.bodyHash, this.ecdsaSignature);

        if (!this.ecKeyPub.verify(this.bodyHash, this.ecdsaSignature)) {
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

    public byte[] getBodyHash() {
        return this.bodyHash;
    }

    public String getBodyHashHexString() {
        return Hex.toHexString(this.bodyHash);
    }

    public ECKey.ECDSASignature getEcdsaSignature() {
        return this.ecdsaSignature;
    }

    public ECKey getEcKeyPub() {
        return this.ecKeyPub;
    }


    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        jsonObject.addProperty("bodyHash", Hex.toHexString(this.bodyHash));

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
