package io.yggdrash.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default Configuration Class.
 */
public class DefaultConfig {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private Config config;

    public DefaultConfig() {
        this(ConfigFactory.empty());
    }

    public DefaultConfig(Config apiConfig) {
        try {
            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            Config referenceConfig = ConfigFactory.parseResources("yggdrash.conf");

            config = apiConfig.withFallback(referenceConfig);
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


}
