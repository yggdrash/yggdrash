package io.yggdrash.common.config;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HexUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class Constants {

    private Constants() {
        throw new IllegalStateException("Constants class");
    }

    public static final int BRANCH_LENGTH = 20;
    public static final int BRANCH_HEX_LENGTH = BRANCH_LENGTH * 2;

    private static final String STEM = "STEM";
    private static final String YEED = "YEED";
    private static final String VALIDATOR = "VALIDATOR";
    public static final String YGGDRASH = "YGGDRASH";
    public static final String BRANCH_ID = "branchId";
    public static final String TX_ID = "txId";
    public static final String BLOCK_ID = "blockId";
    public static final String CONTRACT_VERSION = "contractVersion";

    public static final long TIMESTAMP_2018 = 1514764800000L;
    public static final int MAX_MEMORY = 10000000;

    public static final int HASH_LENGTH = 32;
    public static final int SIGNATURE_LENGTH = 65;

    public static final int TX_BODY_MAX_LENGTH = 10000000; // 10 Mb

    public static final byte[] EMPTY_BRANCH = new byte[BRANCH_LENGTH];
    public static final byte[] EMPTY_HASH = new byte[HASH_LENGTH];
    public static final byte[] EMPTY_SIGNATURE = new byte[SIGNATURE_LENGTH];

    public static final byte[] EMPTY_BYTE8 = new byte[8];
    public static final byte[] EMPTY_BYTE1K = new byte[1024];

    public static final String PBFT_PREPREPARE = "PREPREPA";
    public static final String PBFT_PREPARE = "PREPAREM";
    public static final String PBFT_COMMIT = "COMMITMS";
    public static final String PBFT_VIEWCHANGE = "VIEWCHAN";

    public static final int PASSWORD_MIN = 12;
    public static final int PASSWORD_MAX = 32;

    public static final String YGG_DATA_PATH = "YGG_DATA_PATH";
    public static final String PROPERTY_KEYPATH = "key.path";
    public static final String PROPERTY_KEKPASS = "key.password";
    public static final String PROPERTY_NODE_NAME = "node.name";
    public static final String PROPERTY_NODE_VER = "node.version";
    public static final String PROPERTY_NETWORK_ID = "network.id";
    public static final String PROPERTY_NETWORK_P2P_VER = "network.p2p.version";

    public static final String VALIDATOR_PATH = "yggdrash.validator.path";
    public static final String CONTRACT_PATH = "contract.path";
    public static final String OSGI_PATH = "osgi.path";
    public static final String BRANCH_PATH = "branch.path";
    public static final String DATABASE_PATH = "database.path";

    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }

    private static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffff"
            + "ffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    public final class LIMIT {
        private LIMIT() {
        }

        public static final long BLOCK_SYNC_SIZE = 3 * 1024 * 1024L; // 3MB
    }

    public final class KEY {
        private KEY() {
        }

        public static final String HEADER = "header";
        public static final String SIGNATURE = "signature";
        public static final String BODY = "body";
        public static final String VALIDATOR = "validator";
    }

    // TODO Contract Version fix
    private static final byte[] STEM_CONTRACT = Hex.encode(STEM.getBytes());
    public static final ContractVersion STEM_CONTRACT_VERSION = ContractVersion.of(HexUtil.toHexString(STEM_CONTRACT));
    private static final byte[] YEED_CONTRACT = Hex.encode(YEED.getBytes());
    public static final ContractVersion YEED_CONTRACT_VERSION = ContractVersion.of(HexUtil.toHexString(YEED_CONTRACT));
    private static final byte[] VALIDATOR_CONTRACT = Hex.encode(VALIDATOR.getBytes());
    public static final ContractVersion VALIDATOR_CONTRACT_VERSION = ContractVersion.of(HexUtil.toHexString(VALIDATOR_CONTRACT));

}
