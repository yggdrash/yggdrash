package io.yggdrash.common.config;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.SerializationUtil;

public final class Constants {

    private Constants() {
        throw new IllegalStateException("Constants class");
    }

    public static final int BRANCH_LENGTH = 20;
    public static final int BRANCH_HEX_LENGTH = BRANCH_LENGTH * 2;

    public static final String YGGDRASH = "YGGDRASH";
    public static final String BRANCH_ID = "branchId";
    public static final String TX_ID = "txId";
    public static final String BLOCK_ID = "blockId";
    public static final String CONTRACT_VERSION = "contractVersion";

    public static final long TIMESTAMP_2018 = 1514764800000L;
    public static final int MAX_MEMORY = 10000000;

    public static final int HASH_LENGTH = 32;
    public static final int SIGNATURE_LENGTH = 65;

    public static final byte[] LEVELDB_SIZE_KEY = HashUtil.sha3(SerializationUtil.serializeString("SIZE"));

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

    public static final String CONTRACT_PATH = "contract.path";
    public static final String OSGI_PATH = "osgi.path";
    public static final String BRANCH_PATH = "branch.path";
    public static final String DATABASE_PATH = "database.path";
    public static final String VALIDATOR_DATABASE_PATH = "yggdrash.validator.database.path";
    public static final String VALIDATOR_PATH = "yggdrash.validator.path";

    public final class Limit {
        private Limit() {
        }

        public static final long BLOCK_SYNC_SIZE = 3 * 1024 * 1024L; // 3MB
    }

    public final class Key {
        private Key() {
        }

        public static final String HEADER = "header";
        public static final String SIGNATURE = "signature";
        public static final String BODY = "body";
    }

    public final class ActiveProfiles {
        // environment
        public static final String LOCAL = "local";
        public static final String PROD = "prod";
        // role base
        public static final String VALIDATOR = "validator";
        public static final String GATEWAY = "gateway"; // monitoring only
    }

    private static final String STEM_CONTRACT_STR = "74df17611373672371cb3872e8a5d4a2e8733fb1";
    public static final ContractVersion STEM_CONTRACT_VERSION = ContractVersion.of(STEM_CONTRACT_STR);

    private static final String YEED_CONTRACT_STR = "d79ab8e1d735090d2a7ef4f16d13a910457c0d93";
    public static final ContractVersion YEED_CONTRACT_VERSION = ContractVersion.of(YEED_CONTRACT_STR);
}
