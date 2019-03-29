package io.yggdrash.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import io.yggdrash.common.exception.FailedOperationException;
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
    private static final String YGG_DATA_PATH = "YGG_DATA_PATH";
    private static final String PROPERTY_KEYPATH = "key.path";
    private static final String PROPERTY_KEKPASS = "key.password"; // todo: change to CLI
    private static final String PROPERTY_NODE_NAME = "node.name";
    private static final String PROPERTY_NODE_VER = "node.version";
    private static final String PROPERTY_NETWORK_ID = "network.id";
    private static final String PROPERTY_NETWORK_P2P_VER = "network.p2p.version";

    private static final String CONTRACT_PATH = "contract.path";
    private static final String OSGI_PATH = "osgi.path";
    private static final String BRANCH_PATH = "branch.path";
    private static final String DATABASE_PATH = "database.path";
    protected boolean productionMode;

    private Config config;

    public DefaultConfig() {
        this(ConfigFactory.empty());
    }

    public DefaultConfig(boolean productionMode) {
        this(ConfigFactory.empty(), productionMode);
    }

    public DefaultConfig(Config apiConfig) {
        this(apiConfig, false);
    }

    public DefaultConfig(Config apiConfig, boolean productionMode) {
        try {
            this.productionMode = productionMode;
            Config referenceConfig = getReferenceConfig();

            File file = new File(referenceConfig.getString(YGG_DATA_PATH), "admin.conf");
            Config adminConfig = ConfigFactory.parseFile(file);

            config = apiConfig
                    .withFallback(adminConfig)
                    .withFallback(referenceConfig);

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            config = javaSystemProperties.withFallback(config).resolve();
        } catch (Exception e) {
            logger.error("Can't read config.");
            throw new FailedOperationException(e);
        }
    }

    private Config getReferenceConfig() {
        Config referenceConfig = ConfigFactory.parseResources("yggdrash.conf");

        String userName = System.getProperty("user.name");
        String basePath;
        if ("root".equals(userName)) {
            basePath = System.getProperty("user.dir");
        } else if (productionMode) {
            basePath = System.getProperty("user.home");
        } else {
            basePath = System.getProperty("user.dir");
        }

        String yggDataPath = referenceConfig.getString(YGG_DATA_PATH);
        String path = basePath + File.separator + yggDataPath;
        path = path.replace("//", "/");
        Config prodConfig = ConfigFactory.parseString(YGG_DATA_PATH + " = " + path);

        return prodConfig.withFallback(referenceConfig).resolve();
    }

    public Config getConfig() {
        return config;
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ConfigValue> entry : this.config.entrySet()) {
            if (sb.length() == 0) {
                sb.append("{" + entry.getKey() + ":" + entry.getValue() + "}\n,");
            }
            sb.append("{").append(entry.getKey()).append(":").append(entry.getValue())
                    .append("}").append("\n,");
        }

        return "DefaultConfig{" + sb.substring(0, sb.length() - 1) + "}";
    }

    public boolean isProductionMode() {
        return productionMode;
    }

    public String getKeyPath() {
        return config.getString(PROPERTY_KEYPATH);
    }

    public String getKeyPassword() {
        return config.getString(PROPERTY_KEKPASS);
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

    public String getOsgiPath() {
        return config.getString(OSGI_PATH);
    }

    public String getBranchPath() {
        return config.getString(BRANCH_PATH);
    }

    public String getYggDataPath() {
        return config.getString(YGG_DATA_PATH);
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
