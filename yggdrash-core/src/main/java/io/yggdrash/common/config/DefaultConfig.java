package io.yggdrash.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Default Configuration Class.
 */
public class DefaultConfig {

    private static final Logger logger = LoggerFactory.getLogger("general");
    private static final String PROPERTY_KEYPATH = "key.path";
    private static final String PROPERTY_KEYPASSWORD = "key.password"; // todo: change to CLI
    private static final String PROPERTY_NODE_NAME = "node.name";
    private static final String PROPERTY_NODE_VER = "node.version";
    private static final String PROPERTY_NETWORK_ID = "network.id";
    private static final String PROPERTY_NETWORK_P2P_VER = "network.p2p.version";

    private static final String DATABASE_PATH = "database.path";
    private static final String CONTRACT_PATH = "contract.path";
    private static final String BRANCH_PATH = "branch.path";

    private Config config;

    public DefaultConfig() {
        this(ConfigFactory.empty());
    }

    public DefaultConfig(Config apiConfig) {
        try {
            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            Config referenceConfig = ConfigFactory.parseResources("yggdrash.conf");

            String userDir = System.getProperty("user.dir") + "/.yggdrash";
            File file = new File(userDir, "admin.conf");
            Config adminConfig = ConfigFactory.parseFile(file);

            config = apiConfig
                    .withFallback(adminConfig)
                    .withFallback(referenceConfig);

            config = javaSystemProperties.withFallback(config).resolve();

        } catch (Exception e) {
            logger.error("Can't read config.");
            throw new RuntimeException(e);
        }
    }

    public Config getConfig() {
        return config;
    }

    public String toString() {

        StringBuilder config = null;
        for (Map.Entry<String, ConfigValue> entry : this.config.entrySet()) {
            if (config == null) {
                config = new StringBuilder("{" + entry.getKey() + ":" + entry.getValue() + "}"
                        + "\n,");

            }
            config.append("{").append(entry.getKey()).append(":").append(entry.getValue())
                    .append("}").append("\n,");
        }

        return "DefaultConfig{" + config.substring(0, config.length() - 1) + "}";
    }

    public String getKeyPath() {
        return config.getString(PROPERTY_KEYPATH);
    }

    public String getKeyPassword() {
        return config.getString(PROPERTY_KEYPASSWORD);
    }

    public String getNodeName() {
        return config.getString(PROPERTY_NODE_NAME);
    }

    public String getNodeVersion() {
        return config.getString(PROPERTY_NODE_VER);
    }

    public Network getNetwork() {
        return Network.valueOf(config.getInt(PROPERTY_NETWORK_ID));
    }

    public String getNetworkP2PVersion() {
        return config.getString(PROPERTY_NETWORK_P2P_VER);
    }

    public String getDatabasePath() {
        return config.getString(DATABASE_PATH);
    }

    public String getContractPath() {
        return config.getString(CONTRACT_PATH);
    }

    public String getBranchPath() {
        return config.getString(BRANCH_PATH);
    }

    enum Network {
        MAIN_NET(1), TEST_NET(3);
        private final int code;

        Network(int code) {
            this.code = code;
        }

        static Network valueOf(int code) {
            return Arrays.stream(Network.values()).filter(v -> v.code == code).findFirst()
                    .orElseThrow(NoSuchElementException::new);
        }
    }

}
