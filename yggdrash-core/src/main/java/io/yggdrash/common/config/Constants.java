package io.yggdrash.common.config;

import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.core.contract.ContractId;
import org.spongycastle.util.encoders.Hex;

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

    public static final byte[] EMPTY_BYTE8 = new byte[8];
    public static final byte[] EMPTY_BYTE20 = new byte[20];
    public static final byte[] EMPTY_BYTE32 = new byte[32];

    public static final int TX_HEADER_LENGTH = 84;
    public static final int TX_SIG_LENGTH = 65;
    public static final int TX_BODY_MAX_LENGTH = 10000000; // 10 Mb

    public static final String PBFT_PREPREPARE = "PREPREPA";
    public static final String PBFT_PREPARE = "PREPAREM";
    public static final String PBFT_COMMIT = "COMMITMS";
    public static final String PBFT_VIEWCHANGE = "VIEWCHAN";

    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }

    private static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffff"
            + "ffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    private Constants() {

    }

    public static final byte[] STEM_CONTRACT = Hex.encode(STEM.getBytes());
    public static final ContractId STEM_CONTRACT_ID = ContractId.of(HexUtil.toHexString(STEM_CONTRACT));
    public static final byte[] YEED_CONTRACT = Hex.encode("YEED".getBytes());
    public static final ContractId YEED_CONTRACT_ID = ContractId.of(HexUtil.toHexString(YEED_CONTRACT));
}
