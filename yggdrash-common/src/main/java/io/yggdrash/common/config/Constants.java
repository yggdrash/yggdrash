package io.yggdrash.common.config;

import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.SerializationUtil;

import java.io.File;
import java.math.BigInteger;

public final class Constants {

    private Constants() {
        throw new IllegalStateException("Constants class");
    }

    //yggdrash.conf
    public static final String NODE_NAME = "yggdrash";
    public static final String NODE_VERSION = "0.8.0";

    public static final int BRANCH_LENGTH = 20;
    public static final int BRANCH_HEX_LENGTH = BRANCH_LENGTH * 2;
    public static final int CONTRACT_VERSION_LENGTH = 20;

    public static final String YGGDRASH = "YGGDRASH";
    public static final String BRANCH_ID = "branchId";
    public static final String TX_ID = "txId";
    public static final String BLOCK_ID = "blockId";
    public static final String CONTRACT_VERSION = "contractVersion";

    public static final long TIMESTAMP_2018 = 1514764800000L;
    public static final int MAX_MEMORY = 10000000;
    public static final int MAX_GRPC_MESSAGE_LIMIT = 8192000;

    public static final int HASH_LENGTH = 32;
    public static final int SIGNATURE_LENGTH = 65;

    public static final byte[] LEVELDB_SIZE_KEY = HashUtil.sha3(SerializationUtil.serializeString("SIZE"));

    public static final byte[] EMPTY_BRANCH = new byte[BRANCH_LENGTH];
    public static final byte[] EMPTY_HASH = new byte[HASH_LENGTH];
    public static final byte[] EMPTY_SIGNATURE = new byte[SIGNATURE_LENGTH];

    public static final byte[] EMPTY_BYTE8 = new byte[8];
    public static final byte[] EMPTY_BYTE1K = new byte[1024];

    // TODO: consider changing to enum
    public static final String PBFT_PREPREPARE = "PREPREPA";
    public static final String PBFT_PREPARE = "PREPAREM";
    public static final String PBFT_COMMIT = "COMMITMS";
    public static final String PBFT_VIEWCHANGE = "VIEWCHAN";

    public static final int PASSWORD_MIN = 12;
    public static final int PASSWORD_MAX = 32;

    public static final String YGG_DATA_PATH = "YGG_DATA_PATH";
    public static final String YGG_DEFAULT_FILENAME = "yggdrash.conf";
    public static final String YGG_CONF_PATH = ".yggdrash" + File.separator + YGG_DEFAULT_FILENAME;

    public static final String YGGDRASH_NETWORK_ID = "yggdrash.network.id";
    public static final String YGGDRASH_NETWORK_P2P_VERSION = "yggdrash.network.p2p.version";
    public static final String YGGDRASH_KEY_PATH = "yggdrash.key.path";
    public static final String YGGDRASH_KEY_PASSWORD = "yggdrash.key.password";
    public static final String YGGDRASH_DATABASE_PATH = "yggdrash.database.path";
    public static final String YGGDRASH_ADMIN_MODE = "yggdrash.admin.mode";
    public static final String YGGDRASH_ADMIN_PATH = "yggdrash.admin.path";
    public static final String YGGDRASH_ADMIN_IP = "yggdrash.admin.ip";
    public static final String YGGDRASH_ADMIN_PUBKEY = "yggdrash.admin.pubKey";
    public static final String YGGDRASH_ADMIN_TIMEOUT = "yggdrash.admin.timeout";
    public static final String YGGDRASH_CONTRACT_PATH = "yggdrash.contract.path";
    public static final String YGGDRASH_CONTRACT_URL = "yggdrash.contract.url";
    public static final String YGGDRASH_OSGI_PATH = "yggdrash.osgi.path";
    public static final String YGGDRASH_BRANCH_PATH = "yggdrash.branch.path";

    public static final String NODE_KEY_PATH = "yggdrash.node.key.path";
    public static final String NODE_KEY_PASSWORD = "yggdrash.node.key.password";
    public static final String NODE_GRPC_HOST = "yggdrash.node.grpc.host";
    public static final String NODE_GRPC_PORT = "yggdrash.node.grpc.port";

    public static final String VALIDATOR_PATH = "yggdrash.validator.path";
    public static final String VALIDATOR_INFO = "yggdrash.validator.info";
    public static final String VALIDATOR_PROXYNODE = "yggdrash.validator.proxyNode";
    public static final String VALIDATOR_DATABASE_PATH = "yggdrash.validator.database.path";
    public static final String VALIDATOR_KEY_PATH = "yggdrash.validator.key.path";
    public static final String VALIDATOR_KEY_PASSWORD = "yggdrash.validator.key.password";
    public static final String VALIDATOR_GRPC_HOST_CONF = "yggdrash.validator.host";
    public static final String VALIDATOR_GRPC_PORT_CONF = "yggdrash.validator.port";
    public static final String VALIDATOR_LOG_LEVEL_CONF = "yggdrash.validator.log.level";

    public static final String TIMEOUT_PING_PATH = "yggdrash.node.timeout.ping";
    public static final String TIMEOUT_BLOCK_PATH = "yggdrash.node.timeout.block";
    public static final String TIMEOUT_BLOCKLIST_PATH = "yggdrash.node.timeout.blocklist";
    public static final String TIMEOUT_STATUS_PATH = "yggdrash.node.timeout.status";

    public static final long TIMEOUT_PING = 2;
    public static final long TIMEOUT_BLOCK = 3;
    public static final long TIMEOUT_BLOCKLIST = 10;
    public static final long TIMEOUT_TRANSACTION = 3;
    public static final long TIMEOUT_STATUS = 5;

    public static final long BLOCK_SYNC_COUNT = 10;
    public static final long TRANSACTION_UNCONFIRMED_MAX = 1000;

    // Base currency
    public static final BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);
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
        public static final String BOOTSTRAP = "bootstrap";
        public static final String NODE = "node";
        public static final String VALIDATOR = "validator";
        public static final String GATEWAY = "gateway"; // monitoring only
    }

}
