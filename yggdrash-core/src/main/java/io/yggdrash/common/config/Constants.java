package io.yggdrash.common.config;

import java.math.BigInteger;

public class Constants {

    public static final String STEM = "STEM";
    public static final String YEED = "YEED";
    public static final String BRANCH_ID = "branchId";
    public static final String TX_ID = "txId";
    public static final String BLOCK_ID = "blockId";
    public static final String CONTRACT_ID = "contractId";

    public static final long TIMESTAMP_2018 = 1514764800000L;
    public static final int MAX_MEMORY = 10000000;

    /**
     * Introduced in the Homestead release
     */
    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }

    private static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffff"
            + "ffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    private Constants() {

    }
}
