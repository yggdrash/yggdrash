package io.yggdrash.common.config;

import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.core.contract.ContractVersion;
import java.math.BigInteger;
import org.spongycastle.util.encoders.Hex;

public class Constants {

    public static final String STEM = "STEM";
    public static final String YEED = "YEED";
    public static final String BRANCH_ID = "branchId";
    public static final String TX_ID = "txId";
    public static final String BLOCK_ID = "blockId";
    public static final String CONTRACT_VERSION = "contractVersion";

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

    public static final byte[] STEM_CONTRACT = Hex.encode(STEM.getBytes());
    public static final ContractVersion STEM_CONTRACT_VERSION = ContractVersion.of(HexUtil.toHexString(STEM_CONTRACT));
    public static final byte[] YEED_CONTRACT = Hex.encode("YEED".getBytes());
    public static final ContractVersion YEED_CONTRACT_VERSION = ContractVersion.of(HexUtil.toHexString(YEED_CONTRACT));
}
