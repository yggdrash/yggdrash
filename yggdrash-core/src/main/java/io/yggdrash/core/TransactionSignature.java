package io.yggdrash.core;

import io.yggdrash.crypto.ECKey;
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

    @Override
    public TransactionSignature clone() throws CloneNotSupportedException {
        return (TransactionSignature) super.clone();
    }

}
