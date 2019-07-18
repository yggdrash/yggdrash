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

import static io.yggdrash.common.config.Constants.BRANCH_PATH;
import static io.yggdrash.common.config.Constants.CONTRACT_PATH;
import static io.yggdrash.common.config.Constants.CONTRACT_REPOSITORY_URL;
import static io.yggdrash.common.config.Constants.OSGI_PATH;
import static io.yggdrash.common.config.Constants.PROPERTY_NETWORK_ID;
import static io.yggdrash.common.config.Constants.PROPERTY_NETWORK_P2P_VER;
import static io.yggdrash.common.config.Constants.VALIDATOR_DATABASE_PATH;
import static io.yggdrash.common.config.Constants.VALIDATOR_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_ADMIN_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_DATABASE_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_KEY_PASSWORD;
import static io.yggdrash.common.config.Constants.YGGDRASH_KEY_PATH;
import static io.yggdrash.common.config.Constants.YGG_CONF_PATH;
import static io.yggdrash.common.config.Constants.YGG_DATA_PATH;
import static io.yggdrash.common.config.Constants.YGG_DEFAULT_FILENAME;

/**
 * Default Configuration Class.
 */
public class DefaultConfig {

    private static final Logger logger = LoggerFactory.getLogger("general");

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
            Config referenceConfig = getYggdrashConfig();
            Config adminConfig = getAdminConfig();

            config = adminConfig
                    .withFallback(apiConfig)
                    .withFallback(referenceConfig);

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            config = javaSystemProperties.withFallback(config).resolve();
        } catch (Exception e) {
            logger.error("Can't read config.");
            throw new FailedOperationException(e);
        }
    }

    private Config getYggdrashConfig() {
        Config defaultConfig = ConfigFactory.parseResources(YGG_DEFAULT_FILENAME);

        String basePath;
        if (productionMode) {
            basePath = System.getProperty("user.home");
        } else {
            basePath = System.getProperty("user.dir");
        }
        Config yggdrashConfig = ConfigFactory.parseFile(new File(basePath + File.separator + YGG_CONF_PATH));

        return yggdrashConfig.withFallback(defaultConfig).resolve();
    }

    private Config getAdminConfig() {
        String basePath;
        if (productionMode) {
            basePath = System.getProperty("user.home");
        } else {
            basePath = System.getProperty("user.dir");
        }
        return ConfigFactory.parseFile(new File(basePath + File.separator + YGGDRASH_ADMIN_PATH));
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
        return config.getString(YGGDRASH_KEY_PATH);
    }

    public String getKeyPassword() {
        return config.hasPath(YGGDRASH_KEY_PASSWORD) ? config.getString(YGGDRASH_KEY_PASSWORD) : null;
    }

    public Network getNetwork() {
        return Network.valueOf(config.getInt(PROPERTY_NETWORK_ID));
    }

    public String getNetworkP2PVersion() {
        return config.getString(PROPERTY_NETWORK_P2P_VER);
    }

    public String getDatabasePath() {
        if (config.hasPath(VALIDATOR_DATABASE_PATH)) {
            return config.getString(VALIDATOR_DATABASE_PATH);
        } else {
            return config.getString(YGGDRASH_DATABASE_PATH);
        }
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

    public String getValidatorPath() {
        return config.getString(VALIDATOR_PATH);
    }

    public String getContractRepositoryUrl() {
        return config.getString(CONTRACT_REPOSITORY_URL);
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
