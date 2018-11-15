package io.yggdrash.common.config;

import java.math.BigInteger;

public class Constants {
    public static final String STEM = "STEM";

    private static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffff"
            + "ffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    public static final long TIMESTAMP_2018 = 1514764800000L;

    /**
     * Introduced in the Homestead release
     */
    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }
}
