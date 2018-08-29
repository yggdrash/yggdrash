package io.yggdrash.core;

import io.yggdrash.crypto.ECKey;

public class TransactionSignature {

    private byte[] signature;
    private byte[] data;

    private ECKey.ECDSASignature ecdsaSignature;

    public TransactionSignature() {

    }
}
