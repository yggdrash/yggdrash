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

import static io.yggdrash.common.config.Constants.VALIDATOR_DATABASE_PATH;
import static io.yggdrash.common.config.Constants.VALIDATOR_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_ADMIN_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_BRANCH_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_CONTRACT_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_CONTRACT_URL;
import static io.yggdrash.common.config.Constants.YGGDRASH_DATABASE_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_KEY_PASSWORD;
import static io.yggdrash.common.config.Constants.YGGDRASH_KEY_PATH;
import static io.yggdrash.common.config.Constants.YGGDRASH_NETWORK_ID;
import static io.yggdrash.common.config.Constants.YGGDRASH_NETWORK_P2P_VERSION;
import static io.yggdrash.common.config.Constants.YGGDRASH_OSGI_PATH;
import static io.yggdrash.common.config.Constants.YGG_CONF_PATH;
import static io.yggdrash.common.config.Constants.YGG_DATA_PATH;
import static io.yggdrash.common.config.Constants.YGG_DEFAULT_FILENAME;

/**
 * Default Configuration Class.
 */
public class DefaultConfig {
    private static final Logger log = LoggerFactory.getLogger(DefaultConfig.class);

    protected boolean productionMode;
    private boolean checkTxMode;

    private Config config;

    public DefaultConfig() {
        this(ConfigFactory.empty());
    }

    public DefaultConfig(Config apiConfig, boolean productionMode) {
        this(apiConfig, productionMode, false);
    }

    public DefaultConfig(boolean productionMode, boolean isCheckTxMode) {
        this(ConfigFactory.empty(), productionMode, isCheckTxMode);
    }

    public DefaultConfig(Config apiConfig) {
        this(apiConfig, false, false);
    }

    public DefaultConfig(Config apiConfig, boolean productionMode, boolean checkTxMode) {
        try {
            this.productionMode = productionMode;
            this.checkTxMode = checkTxMode;
            Config referenceConfig = getYggdrashConfig();
            Config adminConfig = getAdminConfig();

            config = adminConfig
                    .withFallback(apiConfig)
                    .withFallback(referenceConfig);

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            config = javaSystemProperties.withFallback(config).resolve();
        } catch (Exception e) {
            log.error("Can't read config.");
            throw new FailedOperationException(e);
        }
    }

    private Config getYggdrashConfig() {
        try {
            Config defaultConfig = ConfigFactory.parseResources(YGG_DEFAULT_FILENAME);
            String configPath = System.getProperty(Constants.YGGDRASH_CONFIG_PATH);
            if (configPath == null || configPath.equals("")) {
                if (productionMode) {
                    configPath = System.getProperty("user.home") + File.separator + YGG_CONF_PATH;
                } else {
                    configPath = System.getProperty("user.dir") + File.separator + YGG_CONF_PATH;
                }
            }
            return ConfigFactory.parseFile(new File(configPath)).withFallback(defaultConfig).resolve();
        } catch (Exception e) {
            log.error("getYggdrashConfig() is failed. {}", e.getMessage());
            return null;
        }
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

    public boolean isCheckTxMode() {
        return checkTxMode;
    }

    public String getKeyPath() {
        return config.getString(YGGDRASH_KEY_PATH);
    }

    public String getKeyPassword() {
        return config.hasPath(YGGDRASH_KEY_PASSWORD) ? config.getString(YGGDRASH_KEY_PASSWORD) : null;
    }

    public Network getNetwork() {
        return Network.valueOf(config.getInt(YGGDRASH_NETWORK_ID));
    }

    public String getNetworkP2PVersion() {
        return config.getString(YGGDRASH_NETWORK_P2P_VERSION);
    }

    public String getDatabasePath() {
        if (config.hasPath(VALIDATOR_DATABASE_PATH)) {
            return config.getString(VALIDATOR_DATABASE_PATH);
        } else {
            return config.getString(YGGDRASH_DATABASE_PATH);
        }
    }

    public String getContractPath() {
        return config.getString(YGGDRASH_CONTRACT_PATH);
    }

    public String getOsgiPath() {
        return config.getString(YGGDRASH_OSGI_PATH);
    }

    public String getBranchPath() {
        return config.getString(YGGDRASH_BRANCH_PATH);
    }

    public String getYggDataPath() {
        return config.getString(YGG_DATA_PATH);
    }

    public String getValidatorPath() {
        return config.getString(VALIDATOR_PATH);
    }

    public String getContractRepositoryUrl() {
        return config.getString(YGGDRASH_CONTRACT_URL);
    }

    public void setConfig(Config config) {
        this.config = config;
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
