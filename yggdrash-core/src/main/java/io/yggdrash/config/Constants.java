package io.yggdrash.config;

import java.math.BigInteger;

public class Constants {

    public static final String PROPERTY_KEYPATH = "key.path";
    public static final String PROPERTY_KEYPASSWORD = "key.password"; // todo: change to CLI
    public static final String PROPERTY_NODE_NAME = "node.name";
    public static final String PROPERTY_NODE_VER = "node.version";
    public static final String PROPERTY_NETWORK_ID = "network.id";
    public static final String PROPERTY_NETWORK_P2P_VER = "network.p2p.version";

    public static final String DATABASE_PATH = "database.path";

    private static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    /**
     * Introduced in the Homestead release
     */
    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }
}
