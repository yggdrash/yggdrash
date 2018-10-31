package io.yggdrash.common.config;

import java.math.BigInteger;

public class Constants {

    public static final String PROPERTY_KEYPATH = "key.path";
    public static final String PROPERTY_KEYPASSWORD = "key.password"; // todo: change to CLI
    static final String PROPERTY_NODE_NAME = "node.name";
    static final String PROPERTY_NODE_VER = "node.version";
    static final String PROPERTY_NETWORK_ID = "network.id";
    static final String PROPERTY_NETWORK_P2P_VER = "network.p2p.version";

    public static final String DATABASE_PATH = "database.path";
    public static final String CONTRACT_PATH = "contract.path";
    public static final String BRANCH_PATH = "branch.path";

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
