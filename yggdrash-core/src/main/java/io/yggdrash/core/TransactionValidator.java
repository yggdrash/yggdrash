package io.yggdrash.core;

import io.yggdrash.crypto.ECKey;

import java.security.SignatureException;

public class TransactionValidator {

    public TransactionValidator() {
    }

    public Boolean txSigValidate(byte[] signDataHash, byte[] signature) throws SignatureException {
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
        ECKey keyFromSig = ECKey.signatureToKey(signDataHash, sig);
        return keyFromSig.verify(signDataHash, sig);
    }

    public void txFormatValidate() {
        // todo transaction format validation
    }
}
